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

        for(int i = 10; i < path.length; i =  i+10){
            System.out.println(path[i].getX() + " " + path[i].getY() + " d " + path[i].getDistanceTo(path[i + 10]));
        }

        System.out.println("Creating Robot");
        RoB1 robot = new RoB1("http://127.0.0.1",50000);

        System.out.println("GET HEADING " + robot.getCurrentHeadingAngle());
        System.out.println("GET BEARING" + robot.getBearingToPoint(path[0]));

        DifferentialDriveRequest dr = new DifferentialDriveRequest();

        for(int i = 10; i < path.length; i = i+10) {
            System.out.println("Start of For");
            dr.setAngularSpeed(0.1);
            dr.setLinearSpeed(0);
            robot.putRequest(dr);
            System.out.println("starting turning whilellooop");
            while( robot.getBearingToPoint(path[i]) - 3 < robot.getHeadingAngle() && robot.getHeadingAngle() > robot.getBearingToPoint(path[i]) + 3){
                System.out.println("Turning" + robot.getHeadingAngle());
                System.out.println("to" + robot.getBearingToPoint(path[i]));
            }
            System.out.println("Ending turning whilloopp, setting forward ging");
            dr.setAngularSpeed(0);
            dr.setLinearSpeed(0.2);
            robot.putRequest(dr);
            System.out.println("Sleeping");
            Thread.sleep(500);
            System.out.println("waking up");

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
            double x = (Double)aPosition.get("X");
            double y = (Double)aPosition.get("Y");
            path[index] = new Position(x, y);
            index++;
        }

        return path;
    }


}
