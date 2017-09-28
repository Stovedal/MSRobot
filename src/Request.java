import java.util.Map;

public interface Request
{
   Map<String, Object> getData();
   String getPath();
}

