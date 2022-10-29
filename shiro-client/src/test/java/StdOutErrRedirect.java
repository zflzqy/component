import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;

/**
 * Created by liyl on 2017/11/24 0024.
 */
public class StdOutErrRedirect {
    private final static Logger logger = LoggerFactory.getLogger(StdOutErrRedirect.class);

    public static void redirectSystemOutAndErrToLog() {
        PrintStream printStreamForOut = createLoggingWrapper(System.out, false);
        PrintStream printStreamForErr = createLoggingWrapper(System.out, true);
        System.setOut(printStreamForOut);
        System.setErr(printStreamForErr);
    }

    public static PrintStream createLoggingWrapper(final PrintStream printStream, final boolean isErr) {
        return new PrintStream(printStream) {
            @Override
            public void print(final String string) {
                if (!isErr){
                    logger.info(string);
                }else{
                    logger.error(string);
                }
            }
            @Override
            public void print(boolean b) {
                if (!isErr){
                    logger.info(Boolean.valueOf(b).toString());
                }else{
                    logger.error(Boolean.valueOf(b).toString());
                }
            }
            @Override
            public void print(char c) {
                if (!isErr){
                    logger.info(Character.valueOf(c).toString());
                }else{
                    logger.error(Character.valueOf(c).toString());
                }
            }
            @Override
            public void print(int i) {
                if (!isErr){
                    logger.info(String.valueOf(i));
                }else{
                    logger.error(String.valueOf(i));
                }
            }
            @Override
            public void print(long l) {
                if (!isErr){
                    logger.info(String.valueOf(l));
                }else{
                    logger.error(String.valueOf(l));
                }
            }
            @Override
            public void print(float f) {
                if (!isErr){
                    logger.info(String.valueOf(f));
                }else{
                    logger.error(String.valueOf(f));
                }
            }
            @Override
            public void print(double d) {
                if (!isErr){
                    logger.info(String.valueOf(d));
                }else{
                    logger.error(String.valueOf(d));
                }
            }
            @Override
            public void print(char[] x) {
                if (!isErr){
                    logger.info(x == null ? null : new String(x));
                }else{
                    logger.error(x == null ? null : new String(x));
                }
            }
            @Override
            public void print(Object obj) {
                if (!isErr){
                    logger.info(obj.toString());
                }else{
                    logger.error(obj.toString());
                }
            }
        };
    }
}
