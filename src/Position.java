
public class Position
{
    private double X;
    private double Y;
    private double Z;


   public Position(double pos[])
   {
      this.X = pos[0];
      this.Y = pos[1];
   }

   public Position(double X, double Y)
   {
       this.X = X;
       this.Y = Y;
   }


    public double getX() { return X; }
    public double getY() { return Y; }

    public double getDistanceTo(Position p)
   {
      return Math.sqrt((X - p.X) * (X - p.X) + (Y - p.Y) * (Y - p.Y));
   }

   // Bearing to another position, relative to 'North'
   // Bearing have several meanings, in this case the angle between
   // north and the position p.
   public double getBearingTo(Position p)
   {
      return Math.atan2(p.Y - Y, p.X - X);
   }
}
