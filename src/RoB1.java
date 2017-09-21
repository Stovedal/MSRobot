import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import com.fasterxml.jackson.databind.*;

/**
 * TestRobot interfaces to the (real or virtual) robot over a network connection.
 * It uses Java -> JSON -> HttpRequest -> Network -> DssHost32 -> Lokarria(Robulab) -> Core -> MRDS4
 *
 * @author thomasj
 */
public class RoB1
{
    private String host;
    private int port;
    private ObjectMapper mapper;
    private double lookAheadDistance;
    private int positionsToSkip;
    private double headingMargin;
    private double linearSpeed;
    private double angularSpeed;
    private LocalizationResponse lr;
    private DifferentialDriveRequest dr;
    private LaserEchoesResponse ler;
    private LaserPropertiesResponse lpr;


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
       this.positionsToSkip = 5;
       this.headingMargin = 5;
       this.lookAheadDistance = 1;
       this.linearSpeed = 1;
       this.angularSpeed = 2;
       this.lr = new LocalizationResponse();
       this.dr = new DifferentialDriveRequest();
       this.ler = new LaserEchoesResponse();
       this.lpr = new LaserPropertiesResponse();

   }

    /**
     * Runs the robot along the given path.
     * @param path Position[]
     */
    public void run( Position[] path ) throws Exception {
        System.out.println("Starting");

        //Set up tracking variables
        double averagei = 0;
        double averageLookAheadDistance = 0;
        double averageSpeed = 0;
        long start = System.currentTimeMillis();

        for(int i = positionsToSkip*5; i < path.length; i = i+positionsToSkip) {
            while( getDistanceToPosition(path[i]) > lookAheadDistance ){
                putRequest(adjustAngularSpeed(path[i]));
                putRequest(adjustLinearSpeed(path[i]));
                int laserPoint = 20;
                if(laserPoint < path.length){adjustLookAheadDistance(path[laserPoint]);}

                //Tracking
                averageSpeed = averageSpeed + (double)dr.getData().get("TargetLinearSpeed");
                averageLookAheadDistance = averageLookAheadDistance + lookAheadDistance;
                averagei = averagei + 1;
            }

        }

        long elapsedTime = System.currentTimeMillis() - start;
        System.out.println("Time of lap " + (elapsedTime) + " milliseconds");
        System.out.println("Averagespeed " + (averageSpeed/averagei));
        System.out.println("Average LookAheadDistance " + (averageLookAheadDistance/averagei));
        System.out.println("done");
        dr.setAngularSpeed(0);
        dr.setLinearSpeed(0);
        putRequest(dr);
    }

    /**
     * Adjusts robots linear-speed after angle of next turn
     * @param nextPosition Position
     */
    private DifferentialDriveRequest adjustLinearSpeed( Position nextPosition ) throws Exception {
        double margin = marginPercentage(getHeadingAngle(), getBearingToPoint(nextPosition));
        if(Double.compare(margin, 0.2) < 0){
            dr.setLinearSpeed(linearSpeed-(linearSpeed * margin));
        } else {
            dr.setLinearSpeed(0);
        }
        return dr;
    }

    /**
     * Adjusts the angular speed of the robot according to the angle to next position
     * @param nextPosition Position
     * @return DifferentialDriveRequest
     * @throws Exception
     */
    private DifferentialDriveRequest adjustAngularSpeed( Position nextPosition) throws Exception {
        if(!checkIfWithinMargin(getHeadingAngle(), getBearingToPoint(nextPosition), headingMargin)) {
            dr.setAngularSpeed(calculateTurn(nextPosition));
        } else {
            dr.setAngularSpeed(0);
        }
        return dr;
    }


    private void adjustLookAheadDistance( Position nextPosition ) throws Exception {
        getResponse(ler);
        double bearing = getBearingToPoint(nextPosition);
        double heading = getHeadingAngle();
        int margin;
        if(!checkIfWithinLimits(bearing, heading, wrapAngle(heading+180))){
            ///bearing-point left of heading
            margin = -marginInDegrees(heading , bearing);
        } else {
            //bearing-point right of heading
            margin = marginInDegrees(bearing, heading);
        }

        int centerPoint = 136 + margin;
        int standard = (int)Math.round(0.5);
        if(standard<2){ standard = 2; }

        if(Math.abs(margin)>30){
            positionsToSkip = Math.round(standard/2);
        } else if(Math.abs(margin) > 10) {
            positionsToSkip = standard;
        } else if(Math.abs(margin) > 2){
            positionsToSkip = 2*standard;
        } else {
            positionsToSkip = 3*standard;
        }

        int laserMargin = 20;
        if(centerPoint<270-laserMargin && centerPoint > laserMargin) {
            double adjusted = ler.getEchoes()[centerPoint];
            for (int i = centerPoint - laserMargin; i < centerPoint + laserMargin; i++) {
                if (Double.compare(ler.getEchoes()[i], adjusted) < 0) {
                    adjusted = ler.getEchoes()[i];
                }
            }
            lookAheadDistance = adjusted * 0.5;
        }
    }

    private double marginPercentage( double heading, double bearing){
        Integer degree =  marginInDegrees(heading, bearing);
        return new Double(degree.toString())/360;
    }

    private int marginInDegrees( double var1, double var2){
        int margin1 = (int) wrapAngle(Math.round( var2 - var1 ));
        int margin2 = (int) wrapAngle(Math.round( var1 - var2 ));
        if (margin1 > margin2){
            return margin2;
        } else {
            return  margin1;
        }
    }

    /**
     * Checks whether the given headingAngle is within an acceptable margin of the bearing
     * @param heading double
     * @param bearing double
     * @return boolean
     */
    private boolean checkIfWithinMargin(double heading, double bearing, double margin ){
        double lowerLimit = wrapAngle(bearing - margin);
        double upperLimit = wrapAngle(bearing + margin);
        return checkIfWithinLimits(heading, lowerLimit, upperLimit);
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

    /**
     * Calculates whether the robot should turn right or left and at what turningspeed
     * @param nextPosition Position
     * @return double
     */
    private double calculateTurn( Position nextPosition) throws Exception {
        double oppositeHeadingAngle = wrapAngle(getHeadingAngle()-180);
        double margin = marginPercentage(getBearingToPoint(nextPosition), getHeadingAngle());
        double speed;
        if(Double.compare(margin, 0.1) < 0 ){
            speed = 0.5;
        } else {
            speed = angularSpeed;
        }
        if(!checkIfWithinLimits(getBearingToPoint(nextPosition),getHeadingAngle(),oppositeHeadingAngle)){
            return -speed;
        } else {
            return speed;
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

    private void nudgeLeft() throws Exception {
        dr.setAngularSpeed(angularSpeed);
        putRequest(dr);
    }

    private void nudgeRight() throws Exception {
        dr.setAngularSpeed(-angularSpeed);
        putRequest(dr);
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

