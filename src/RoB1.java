import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * RoBi interfaces to the (real or virtual) robot over a network connection.
 * It uses Java -> JSON -> HttpRequest -> Network -> DssHost32 -> Lokarria(Robulab) -> Core -> MRDS4
 *
 * @author thomasj
 */
public class RoB1
{
    private String host;
    private int port;
    private ObjectMapper mapper = new ObjectMapper();
    private double lookAheadDistance = 0.4;
    private int positionsToSkip = 5;
    private double headingMargin = 5;
    private double linearSpeed = 1;
    private double angularSpeed = 2;
    private LocalizationResponse lr = new LocalizationResponse();
    private DifferentialDriveRequest dr = new DifferentialDriveRequest();
    private LaserEchoesResponse ler = new LaserEchoesResponse();

   /**
    * Create a robot connected to host "host" at port "port"
    * @param host normally http://127.0.0.1
    * @param port normally 50000
    */
   public RoB1(String host, int port) {
       this.host = host;
       this.port = port;
   }

    /**
     * Runs the robot along the given path.
     * @param path Position[]
     */
    public void run( Position[] path, double speed ) throws Exception {
        linearSpeed = speed;
        long start = System.currentTimeMillis();
        int laserPositionsToSkip;
        int lastPosition = path.length-1;
        //Start moving
        for(int i = 0; i < path.length; i = i+positionsToSkip) {
            while(Double.compare(getDistanceToPosition(path[i]), lookAheadDistance)>0 ){
                getResponse(ler);
                adjustAngularSpeed(path[i]);
                adjustLinearSpeed(path[i]);
                laserPositionsToSkip = (int)Math.round(20*lookAheadDistance+5) ;
                if(laserPositionsToSkip+i < path.length){
                    scan(path[i+laserPositionsToSkip]);
                }
            }
        }

        //Move to last position
        while( Double.compare(getDistanceToPosition(path[lastPosition]), 0.4) < 0 ){
            getResponse(ler);
            adjustAngularSpeed(path[lastPosition]);
            adjustLinearSpeed(path[lastPosition]);
        }

        //Stop and print time of lap
        long elapsedTime = System.currentTimeMillis() - start;
        System.out.println("Time of lap " + (elapsedTime) + " milliseconds");
        dr.setAngularSpeed(0);
        dr.setLinearSpeed(0);
        putRequest(dr);
    }

    /**
     * Adjusts robots linear-speed after angle of next turn
     * @param nextPosition Position
     */
    private void adjustLinearSpeed( Position nextPosition ) throws Exception {
        double margin = marginPercentage(getHeadingAngle(), getBearingToPoint(nextPosition));
        if(Double.compare(margin, 0.20) < 0){
            dr.setLinearSpeed(linearSpeed);

        } else {
            dr.setLinearSpeed(0);
        }
        putRequest(dr);
    }

    /**
     * Adjusts the angular speed of the robot according to the angle to next position
     * @param nextPosition Position
     * @throws Exception
     */
    private void adjustAngularSpeed( Position nextPosition) throws Exception {
        if(!checkIfWithinMargin(getHeadingAngle(), getBearingToPoint(nextPosition), headingMargin)){
            dr.setAngularSpeed(calculateTurn(nextPosition));
            putRequest(dr);
        } else {
            avoidObstacles();
        }
    }

    /**
     * Scans the surroundings and adjusts variables accordingly
     * @param nextPosition Position
     * @throws Exception
     */
    private void scan( Position nextPosition ) throws Exception {
        int margin = headingToBearingMargin(nextPosition);
        adjustLookAheadDistance(margin);
        adjustPositionsToSkip(margin);
    }

    /**
     * Uses laser to avoid obstacles to the left and right
     * @throws Exception
     */
    private void avoidObstacles() throws Exception {
        double allowedMargin = lookAheadDistance;
        int angle = 20;
        double speed = 0.7;
        if(Double.compare(lookAheadDistance, 0.7)<0) {
            allowedMargin = 0.7;
        }
        double leftObstacle = distanceToObstacle(-angle,angle);
        double rightObstacle = distanceToObstacle(angle,angle);
        if(Double.compare(leftObstacle, allowedMargin ) < 0
                || Double.compare(rightObstacle, allowedMargin ) < 0){
            if(Double.compare(leftObstacle,rightObstacle) < 0){
                dr.setAngularSpeed(speed-distanceToObstacle(-angle,angle));
            } else  {
                dr.setAngularSpeed(-speed + distanceToObstacle(angle,angle));
            }
        } else {
            dr.setAngularSpeed(0);
        }
        putRequest(dr);
    }

    /**
     * Calculates margin between heading and bearing in degrees
     * @param nextPosition Position
     * @return int
     * @throws Exception
     */
    private int headingToBearingMargin( Position nextPosition ) throws Exception {
        double bearing = getBearingToPoint(nextPosition);
        double heading = getHeadingAngle();
        if(!checkIfWithinLimits(bearing, heading, wrapAngle(heading+180))){
            ///bearing-point left of heading
            return -marginInDegrees(heading , bearing);
        } else {
            //bearing-point right of heading
            return marginInDegrees(bearing, heading);
        }
    }

    /**
     * Gets the distance to closest obstacle within given margin of given angle
     * @param angle int
     * @param margin int
     * @return double
     * @throws Exception
     */
    private double distanceToObstacle(int angle, int margin ) throws Exception {
        int centerPoint = 136 + angle;
        double distance = ler.getEchoes()[centerPoint];
        for (int i = centerPoint - margin; i < centerPoint + margin; i++) {
            if (Double.compare(ler.getEchoes()[i], distance) < 0) {
                distance = ler.getEchoes()[i];
            }
        }
        return distance;
    }

    /**
     * Adjusts lookAheadDistance according to closest obstacle in bearing
     * @param headingToBearingMargin int
     * @throws Exception
     */
    private void adjustLookAheadDistance( int headingToBearingMargin ) throws Exception {
        int centerPoint = 136 + headingToBearingMargin;
        int laserMargin = 40;
        if(centerPoint<270-laserMargin && centerPoint > laserMargin) {
            lookAheadDistance = limitLookAheadDistance(distanceToObstacle(headingToBearingMargin, laserMargin));
        }
    }

    private double limitLookAheadDistance(double lookAheadDistance){
        if(Double.compare(lookAheadDistance,1.5)>0){
            return 1.5;
        } else if(Double.compare(lookAheadDistance,0)<0.1){
            return 0.1;
        } else {
            return lookAheadDistance;
        }
    }

    /**
     * Adjusts positionsToSkip according to how tight the turn to next position is
     * @param margin int
     */
    private void adjustPositionsToSkip(int margin){
        if(margin<5){
            positionsToSkip = 10;
        } else if(margin<20){
            positionsToSkip = 5;
        } else {
            positionsToSkip = 2;
        }
    }


    /**
     * Calculates margin between heading and bearing in percentages of whole
     * radius of 360.
     * @param heading double
     * @param bearing double
     * @return double
     */
    private double marginPercentage( double heading, double bearing){
        Integer degree =  marginInDegrees(heading, bearing);
        return new Double(degree.toString())/360;
    }

    /**
     * Calculates margin between heading and bearing in degrees
     * @param var1 double
     * @param var2 double
     * @return int
     */
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
     * Checks whether the given headingAngle is within given margin of the bearing.
     * @param heading double
     * @param bearing double
     * @param margin double
     * @return boolean
     */
    private boolean checkIfWithinMargin(double heading, double bearing, double margin ){
        double lowerLimit = wrapAngle(bearing - margin);
        double upperLimit = wrapAngle(bearing + margin);
        return checkIfWithinLimits(heading, lowerLimit, upperLimit);
    }

    /**
     * If necessary, wraps an angle to the 0-360 degree range
     * @param angle double
     * @return double
     */
    private double wrapAngle( double angle){
        if(Double.compare(angle, 0) < 0){
            angle = angle + 360;
            return angle;
        }
        if(Double.compare(angle, 360) > 0){
            angle = angle - 360;
            return angle;
        }
        return angle;
    }

    /**
     * Calculates whether the robot should turn right or left and at what angular speed.
     * @param nextPosition Position
     * @return double
     */
    private double calculateTurn( Position nextPosition) throws Exception {
        double oppositeHeadingAngle = wrapAngle(getHeadingAngle()-180);
        double margin = marginPercentage(getBearingToPoint(nextPosition), getHeadingAngle());
        double speed;

        if(Double.compare(margin, 0.07) < 0 ){
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
     * Checks if angle is within given limits
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
    * @return double
    */
   private double getHeadingAngle() throws Exception
   {
       getResponse(lr);
       double e[] = lr.getOrientation();
       return convertToDegrees(2 * Math.atan2(e[3], e[0]));
   }

    /**
     * Converts angle from robot to degrees.
     * @param angle double
     * @return double
     */
    private double convertToDegrees( double angle ) {
        return wrapAngle(Math.toDegrees(angle));
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

