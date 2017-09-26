import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.Collection;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws Exception {
        Position[] path = readPath("C:\\Users\\Sofia\\IdeaProjects\\MSRobot\\src\\SofaAndBench.json");
        RoB1 robot = new RoB1("http://127.0.0.1", 50000);
        for(int i = 0 ; i <1; i++) {
            robot.run(path);
        }
    }


    /**
     * Reads path given as json and returns it as an array of Positions
     * @param pathString String
     * @return Position[]
     * @throws Exception
     */
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
