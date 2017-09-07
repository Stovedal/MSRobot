/**
 * Created by Sofia on 2017-09-07.
 */
public class Pose {

    private  Orientation Orientation;
    private Position Position;

    public Pose(Orientation orientation, Position position) {
        Orientation = orientation;
        Position = position;
    }

    public Orientation getOrientation() {
        return Orientation;
    }

    public void setOrientation(Orientation orientation) {
        Orientation = orientation;
    }

    public Position getPosition() {
        return Position;
    }

    public void setPosition(Position position) {
        Position = position;
    }
}
