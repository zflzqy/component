import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.io.IOException;
import java.util.Date;

/**
 * @Author: zfl
 * @Date: 2022-08-13-9:27
 * @Description:
 */
public class test {
    public static void main(String[] args) throws IOException {
//        File file = new File("C:\\zfl\\code\\myApp\\src\\main\\resources\\application.properties");
//        ParameterTool propertiesFile = ParameterTool.fromPropertiesFile(file);
//        String jsonFromProperties = new PropertiesToJsonConverter().convertToJson(propertiesFile.getProperties());
//        System.out.println(jsonFromProperties);
//        System.out.println(propertiesFile.toMap());
        String s ="110000000000";
        System.out.println(StringUtils.substring(s,0,2));
        System.out.println(DateFormatUtils.format(new Date(),""));

    }
}
