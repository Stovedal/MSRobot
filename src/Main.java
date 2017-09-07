import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class Main {


    public static void main(String[] args) throws Exception {
        System.out.println("Creating Robot");
        RoB1 robot = new RoB1("http://127.0.0.1",50000);

        //Read Json into Step-List
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Step>>() {}.getType();
        List<Step> path = gson.fromJson(new FileReader("C:\\Users\\Sofia\\IdeaProjects\\MSRobot\\src\\path.json"), listType);



        System.out.println(path.get(5).getPose().getOrientation().getX());

        System.out.println("Creating response");
        LocalizationResponse lr = new LocalizationResponse();

        System.out.println("Creating laserResponse");
        LaserEchoesResponse laser = new LaserEchoesResponse();

        System.out.println("Creating request");
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

            robot.getResponse(laser);

            double angle = robot.getHeadingAngle(lr);
            System.out.println("heading = " + angle);

            double [] position = robot.getPosition(lr);
            System.out.println("position = " + position[0] + ", " + position[1]);

            System.out.println("laserechoe " + laser.getEchoes());
        }

        // set up request to stop the robot
        dr.setLinearSpeed(0);
        dr.setAngularSpeed(0);

        System.out.println("Stop robot");
        rc = robot.putRequest(dr);
        System.out.println("Response code " + rc);

    }


    private static StringBuffer getPosition() throws IOException {
        URL url = new URL("http://localhost:50000/lokarria/localization");
        HttpURLConnection connection = null;
        try {

        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
            System.out.println(connection.getRequestMethod());
        //Send request
        DataOutputStream wr = new DataOutputStream (
                connection.getOutputStream());
            wr.close();

        //Get Response
            System.out.println(connection.getPermission());
        InputStream is = connection.getInputStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = rd.readLine()) != null) {
            content.append(inputLine);
        }
        rd.close();
        return content;
    } catch (Exception e) {
        e.printStackTrace();
        return null;
    } finally {
        if (connection != null) {
            connection.disconnect();
        }
    }
}

}
        /*
        public static String executePost(String targetURL, String urlParameters) {
          HttpURLConnection connection = null;

          try {
            //Create connection
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");

            connection.setRequestProperty("Content-Length",
                Integer.toString(urlParameters.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream (
                connection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.close();

            //Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
            String line;
            while ((line = rd.readLine()) != null) {
              response.append(line);
              response.append('\r');
            }
            rd.close();
            return response.toString();
          } catch (Exception e) {
            e.printStackTrace();
            return null;
          } finally {
            if (connection != null) {
              connection.disconnect();
            }
          }
        }*/


