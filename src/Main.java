import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class Main {

    private static Position[] path;

    public static void main(String[] args) throws Exception {


        /*
        //Read Json into Step-List
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Step>>() {}.getType();
        List<Step> path = gson.fromJson(new FileReader("C:\\Users\\Sofia\\IdeaProjects\\MSRobot\\src\\path.json"), listType);
        */
        path = readPath("C:\\Users\\Sofia\\IdeaProjects\\MSRobot\\src\\path.json");

        int positionsToJump = 50;

        for(int i = positionsToJump; i < path.length; i =  i+positionsToJump){
            System.out.println(path[i].getX() + " " + path[i-positionsToJump].getY() + " d " + path[i].getDistanceTo(path[i]));
        }

        System.out.println("Creating Robot");
        RoB1 robot = new RoB1("http://127.0.0.1",50000);

        System.out.println("GET HEADING " + robot.getCurrentHeadingAngle());
        System.out.println("GET BEARING" + robot.getBearingToPoint(path[0]));

        DifferentialDriveRequest dr = new DifferentialDriveRequest();

        double angleMargin = 5;
        double distanceMargin = 1;

        for(int i = positionsToJump; i < path.length; i = i+positionsToJump) {
            System.out.println("Start of For");
            dr.setAngularSpeed(0.2);
            dr.setLinearSpeed(0);
            robot.putRequest(dr);
            System.out.println("starting turning whilelooop");
            double lowerLimit = robot.getBearingToPoint(path[i]) - angleMargin;
            double upperLimit = robot.getBearingToPoint(path[i]) + angleMargin;
            double heading = robot.getHeadingAngle();
            System.out.println(" boundary 1 " + lowerLimit + " boundary2 " + upperLimit + " heading " + heading);
            while(checkIfWithinLimits(robot.getHeadingAngle(), robot.getBearingToPoint(path[i]))){
            }
            dr.setAngularSpeed(0);
            dr.setLinearSpeed(0.2);
            robot.putRequest(dr);
            while( robot.getDistanceToPosition(path[i]) > distanceMargin ){
                System.out.println("movin " + robot.getDistanceToPosition(path[i]));
            }

        }
        dr.setAngularSpeed(0);
        robot.putRequest(dr);


    }

    /**
     * Checks wether the given
     * @param headingAngle
     * @param bearingAngle
     * @return boolean
     */
    private static boolean checkIfWithinLimits(double headingAngle, double bearingAngle){
        double margin = 5;
        double lowerLimit = bearingAngle - margin;
        double upperLimit = bearingAngle + margin;
        boolean condition = Double.compare(headingAngle, lowerLimit) > 0;
        System.out.println(condition + " heading " + headingAngle + " lowerlimit " + lowerLimit);
        if(Double.compare(headingAngle, lowerLimit) > 0 && Double.compare(headingAngle, upperLimit) < 0 ){
            return false;
        } else {
            return true;
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
