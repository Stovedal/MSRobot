import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.Collection;
import java.util.Map;

public class Main {

    private static Position[] path;

    public static void main(String[] args) throws Exception {

        path = readPath("C:\\Users\\Sofia\\IdeaProjects\\MSRobot\\src\\SofaAndBench.json");

        int positionsToJump = 10;
        double lookAheadDistance = 1;

        RoB1 robot = new RoB1("http://127.0.0.1",50000);
        DifferentialDriveRequest dr = new DifferentialDriveRequest();

        for(int i = positionsToJump; i < path.length; i = i+positionsToJump) {
            System.out.println("Start of For");
            dr.setAngularSpeed(calculateTurn(robot.getHeadingAngle(), robot.getBearingToPoint(path[i])));
            dr.setLinearSpeed(0.5);
            robot.putRequest(dr);
            while(!checkHeading(robot.getHeadingAngle(), robot.getBearingToPoint(path[i])) && robot.getDistanceToPosition(path[i]) > lookAheadDistance){
            }
            dr.setAngularSpeed(0);
            robot.putRequest(dr);
            while( robot.getDistanceToPosition(path[i]) > lookAheadDistance ){
            }

        }
        System.out.println("done");
        dr.setAngularSpeed(0);
        dr.setLinearSpeed(0);
        robot.putRequest(dr);


    }

    /**
     * Checks wether the given headingAngle is within an acceptable margin of the bearingAngle
     * @param headingAngle double
     * @param bearingAngle double
     * @return boolean
     */
    private static boolean checkHeading(double headingAngle, double bearingAngle){
        double margin = 5;
        double lowerLimit = wrapAngle(bearingAngle - margin);
        double upperLimit = wrapAngle(bearingAngle + margin);
        return checkIfWithinLimits(headingAngle, lowerLimit, upperLimit);
    }

    /**
     * Wraps an angle to the 0-360 degree spectrum
     * @param limit double
     * @return double
     */
    private static double wrapAngle( double limit){
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


    private static double calculateTurn( double headingAngle, double bearingAngle){
        double speed = 1;
        double oppositeHeadingAngle = wrapAngle(headingAngle-180);
        if(!checkIfWithinLimits(bearingAngle,headingAngle,oppositeHeadingAngle)){
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
    private static boolean checkIfWithinLimits(double angle, double lowerLimit, double upperLimit ){
        if(Double.compare(lowerLimit, upperLimit) > 0){
            return (Double.compare(angle, lowerLimit) > 0 || Double.compare(angle, upperLimit) < 0);
        } else {
            return (Double.compare(angle, lowerLimit) > 0 && Double.compare(angle, upperLimit) < 0);
        }
    }

    private static Position[] readPath( String pathString ) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(
                new FileInputStream(new File(pathString))));
        ObjectMapper mapper = new ObjectMapper();
        // read the path from the file
        Collection <Map<String, Object>> data =
                (Collection<Map<String, Object>>) mapper.readValue(in, Collection.class);
        int nPoints = data.size();
        Position[] path = new Position[nPoints];
        int index = 0;
        for (Map<String, Object> point : data)
        {
            Map<String, Object> pose = (Map<String, Object>)point.get("Pose");
            Map<String, Object> aPosition = (Map<String, Object>)pose.get("Position");
            Map<String, Object> anOrientation = (Map<String, Object>)pose.get("Orientation");
            double x = (Double)aPosition.get("X");
            double y = (Double)aPosition.get("Y");
            double Ox = (Double) anOrientation.get("X");
            double Oy = (Double) anOrientation.get("Y");
            double Oz = (Double) anOrientation.get("Z");
            double Ow = (Double) anOrientation.get("W");
            Orientation orientation = new Orientation(Ow, Ox, Oy, Oz);

            path[index] = new Position(x, y);
            index++;
        }

        return path;
    }


}
