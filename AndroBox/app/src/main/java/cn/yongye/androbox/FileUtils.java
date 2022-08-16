package cn.yongye.androbox;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtils {

    /**
     * save assets file into local.
     * @param assetsPath
     * @param destPath
     */
    public static void dumpFile(String assetsPath, String destPath){
        File destFile = new File(destPath);
        if(!new File(destFile.getParent()).exists())
            new File(destFile.getParent()).mkdirs();
        try {
            if(!destFile.exists())
                destFile.createNewFile();
            InputStream in =  MyApp.getInstance().getAssets().open(assetsPath);
            FileOutputStream out = new FileOutputStream(destFile);
            byte[] tmpbt = new byte[1024];
            int readCount = 0;
            while((readCount=in.read(tmpbt)) != -1){
                out.write(tmpbt, 0, readCount);
            }
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
