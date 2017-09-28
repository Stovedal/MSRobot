import java.util.Map;

public interface Response
{
   void setData(Map<String, Object> data);
   String getPath();
   long getTimestamp();
}
