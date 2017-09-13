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
   }


   /**
    * Extract the robot heading from the response
    * @param
    * @return angle in degrees
    */
   double getHeadingAngle() throws Exception
   {
       LocalizationResponse lr = new LocalizationResponse();
       getResponse(lr);
       double e[] = lr.getOrientation();
       double angle = 2 * Math.atan2(e[3], e[0]);
       return angle * 180 / Math.PI;
   }

    /**
     * Get Bearing to Point
     * @param
     * @return
     */
    double getBearingToPoint(Position positionPoint) throws Exception {
       return getCurrentPosition().getBearingTo(positionPoint);
    }

   /**
    * Extract the current position
    * @param
    * @return Position
    */
   Position getCurrentPosition() throws Exception
   {
       LocalizationResponse lr = new LocalizationResponse();
       getResponse(lr);
       return lr.getPosition();
   }

    /**
     * Extract the current HeadingAngle
     * @param
     * @return Position
     */
    double getCurrentHeadingAngle() throws Exception
    {
        LocalizationResponse lr = new LocalizationResponse();
        getResponse(lr);
        return lr.getHeadingAngle();
    }

    /**
     * Get Distance to point
     * @param
     * @return double
     */
    double getDistanceToPosition(Position positionPoint) throws Exception {
        return getCurrentPosition().getDistanceTo(positionPoint);

    }

    /**
     * Turn to angle
     */

    void turnTo(double angle) throws Exception {
        double currentHeadingAngle = getCurrentHeadingAngle();
        DifferentialDriveRequest dr = new DifferentialDriveRequest();
        dr.setAngularSpeed(0.2);
        dr.setLinearSpeed(0);
        putRequest(dr);
        while(currentHeadingAngle > angle - Math.PI*0.05 &&  currentHeadingAngle < angle + Math.PI*0.05){
            System.out.println("Turning " + currentHeadingAngle + " to " + angle);
            currentHeadingAngle = getCurrentHeadingAngle();
        }
        dr.setAngularSpeed(0);
        putRequest(dr);
        System.out.println("Stopping " + currentHeadingAngle + " " + angle);
    }


    /**
    * Send a request to the robot.
    * @param r request to send
    * @return response code from the connection (the web server)
    * @throws Exception
    */
   public int putRequest(Request r) throws Exception
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
   public Response getResponse(Response r) throws Exception
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

    public void moveToPos(Step newPos) throws Exception {
        DifferentialDriveRequest dr = new DifferentialDriveRequest();
        LocalizationResponse lr = new LocalizationResponse();
        Step startStep = (Step) getResponse(lr);
        System.out.println(startStep);
    }

}

