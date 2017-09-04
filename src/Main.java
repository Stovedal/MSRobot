import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class Main {


    public static void main(String[] args) throws IOException {
        System.out.println("Hello, world!");
        StringBuffer position = getPosition();
        System.out.println(position);

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


