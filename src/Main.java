import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class Main {

    private static Position[] path;

    public static void main(String[] args) throws Exception {
        System.out.println("Creating Robot");
        RoB1 robot = new RoB1("http://127.0.0.1",50000);

        /*
        //Read Json into Step-List
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Step>>() {}.getType();
        List<Step> path = gson.fromJson(new FileReader("C:\\Users\\Sofia\\IdeaProjects\\MSRobot\\src\\path.json"), listType);
        */
        path = readPath("C:\\Users\\Sofia\\IdeaProjects\\MSRobot\\src\\path.json");


        System.out.println("Creating response");
        LocalizationResponse lr = new LocalizationResponse();

        System.out.println("Creating Localizationrequest");
        robot.getResponse(lr);
        System.out.println("Received LR " + lr.getPosition().getX() );
        /*
        DifferentialDriveRequest dr = new DifferentialDriveRequest();

        // set up the request to move in a circle
        dr.setAngularSpeed(Math.PI * 0.25 );
        dr.setLinearSpeed(-1);

        System.out.println("Start to move robot");
        int rc = robot.putRequest(dr);
        System.out.println("Response code " + rc);

        for (int i = 0; i < 1; i++)
        {
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException ex) {}

            // ask the robot about its position and angle
            robot.getResponse(lr);

            double angle = robot.getHeadingAngle(lr);
            System.out.println("heading = " + angle);

            double [] position = robot.getPosition(lr);
            System.out.println("position = " + position[0] + ", " + position[1]);

        }

        // set up request to stop the robot
        dr.setLinearSpeed(0);
        dr.setAngularSpeed(0);

        System.out.println("Stop robot");
        rc = robot.putRequest(dr);
        System.out.println("Response code " + rc);
        */
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
