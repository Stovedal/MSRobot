import java.awt.image.DirectColorModel;
import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Jar file for JSON support
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.annotation.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import static java.lang.Math.abs;

/**
 * TestRobot interfaces to the (real or virtual) robot over a network connection.
 * It uses Java -> JSON -> HttpRequest -> Network -> DssHost32 -> Lokarria(Robulab) -> Core -> MRDS4
 *
 * @author thomasj
 */
public class RoB1
{
    private String host;                // host and port numbers
    private int port;
    private ObjectMapper mapper;        // maps between JSON and Java structures
    private RoB1 robot;
    private double lookAheadDistance;
    private int positionsToSkip;
    private double headingMargin;
    private double linearSpeed;
    private double angularSpeed;


   /**
    * Create a robot connected to host "host" at port "port"
    * @param host normally http://127.0.0.1
    * @param port normally 50000
    */
   public RoB1(String host, int port)
   {
       this.mapper = new ObjectMapper();
       this.host = host;
       this.port = port;
       this.robot = this;
       this.positionsToSkip = 5;
       this.headingMargin = 5;
       this.lookAheadDistance = 1;
       this.linearSpeed = 0.5;
       this.angularSpeed = 0.5;

   }

    /**
     * Runs the robot along the given path.
     * @param path Position[]
     */
    public void run( Position[] path ) throws Exception {
        DifferentialDriveRequest dr = new DifferentialDriveRequest();
        for(int i = positionsToSkip; i < path.length; i = i+positionsToSkip) {
            dr.setAngularSpeed(calculateTurn(robot.getHeadingAngle(), robot.getBearingToPoint(path[i])));
            dr.setLinearSpeed(linearSpeed);
            robot.putRequest(dr);
            //while angle isn't accurate enough, turn in most sufficient direction
            while(!checkHeading(robot.getHeadingAngle(), robot.getBearingToPoint(path[i])) && robot.getDistanceToPosition(path[i]) > lookAheadDistance){
            }
            dr.setAngularSpeed(0);
            robot.putRequest(dr);
            //When angle is accurate enough, move forward until next position is within lookAheadDistance
            while( robot.getDistanceToPosition(path[i]) > lookAheadDistance ){
            }

        }
        System.out.println("done");
        dr.setAngularSpeed(0);
        dr.setLinearSpeed(0);
        robot.putRequest(dr);
    }


    /**
     * Checks whether the given headingAngle is within an acceptable margin of the bearingAngle
     * @param headingAngle double
     * @param bearingAngle double
     * @return boolean
     */
    private boolean checkHeading(double headingAngle, double bearingAngle){
        double lowerLimit = wrapAngle(bearingAngle - headingMargin);
        double upperLimit = wrapAngle(bearingAngle + headingMargin);
        return checkIfWithinLimits(headingAngle, lowerLimit, upperLimit);
    }

    /**
     * Wraps an angle to the 0-360 degree spectrum
     * @param limit double
     * @return double
     */
    private double wrapAngle( double limit){
        if(Double.compare(limit, 0) < 0){
            limit = limit + 360;
            return limit;
        }
        if(Double.compare(limit, 360) > 0){
            limit = limit - 360;
            return limit;
        }
        return limit;
    }


    private double calculateTurn( double headingAngle, double bearingAngle){
        double oppositeHeadingAngle = wrapAngle(headingAngle-180);
        if(!checkIfWithinLimits(bearingAngle,headingAngle,oppositeHeadingAngle)){
            return -angularSpeed;
        } else {
            return angularSpeed;
        }

    }

    /**
     * Checks if angle is within limits
     * @param angle double
     * @param lowerLimit double
     * @param upperLimit double
     * @return boolean
     */
    private boolean checkIfWithinLimits(double angle, double lowerLimit, double upperLimit ){
        if(Double.compare(lowerLimit, upperLimit) > 0){
            return (Double.compare(angle, lowerLimit) > 0 || Double.compare(angle, upperLimit) < 0);
        } else {
            return (Double.compare(angle, lowerLimit) > 0 && Double.compare(angle, upperLimit) < 0);
        }
    }



    /**
    * Extract the robot heading from the response.
    * @return angle in degrees
    */
   private double getHeadingAngle() throws Exception
   {
       LocalizationResponse lr = new LocalizationResponse();
       getResponse(lr);
       double e[] = lr.getOrientation();
       double angle = 2 * Math.atan2(e[3], e[0]);
       angle = convertToDegrees(angle);
       return angle;

   }


    private double convertToDegrees( double angle ) {
        angle = Math.toDegrees(angle);
        if(angle<0){
            return angle+360;
        } else {
            return angle;
        }
    }

    /**
     * Get Bearing to Point
     * @param position Position
     * @return double
     */
    private double getBearingToPoint(Position position) throws Exception {
       return convertToDegrees(getCurrentPosition().getBearingTo(position));
    }


    /**
    * Extract the current position
    * @return Position
    */
    private Position getCurrentPosition() throws Exception {
        LocalizationResponse lr = new LocalizationResponse();
        getResponse(lr);
        return lr.getPosition();
    }

    /**
     * Get Distance to point
     * @param position Position
     * @return double
     */
    private double getDistanceToPosition(Position position) throws Exception {
        return getCurrentPosition().getDistanceTo(position);

    }


    /**
    * Send a request to the robot.
    * @param r request to send
    * @return response code from the connection (the web server)
    * @throws Exception
    */
   private int putRequest(Request r) throws Exception
   {
      URL url = new URL(host + ":" + port + r.getPath());

      HttpURLConnection connection = (HttpURLConnection)url.openConnection();

      connection.setDoOutput(true);

      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setUseCaches (false);

      OutputStreamWriter out = new OutputStreamWriter(
            connection.getOutputStream());

      // construct a JSON string
      String json = mapper.writeValueAsString(r.getData());

      // write it to the web server
      out.write(json);
      out.close();

      // wait for response code
      int rc = connection.getResponseCode();

      return rc;
   }

   /**
    * Get a response from the robot
    * @param r response to fill in
    * @return response same as parameter
    * @throws Exception
    */
   private Response getResponse(Response r) throws Exception
   {
      URL url = new URL(host + ":" + port + r.getPath());

      // open a connection to the web server and then get the resulting data
      URLConnection connection = url.openConnection();
      BufferedReader in = new BufferedReader(new InputStreamReader(
            connection.getInputStream()));

      // map it to a Java Map
      Map<String, Object> data = mapper.readValue(in, Map.class);
      r.setData(data);
      in.close();

      return r;
   }

}

