package com.zhangyh.shapefile.shapefile;

import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.style.Style;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.styling.SLD;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.data.JFileDataStoreChooser;

import java.io.File;
import java.util.logging.Logger;

/**
 * @author zhangyh
 * @Date 2025/12/3 13:49
 * @desc
 */
public class ShapeFileOpen {
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(ShapeFileOpen.class);
    /**
     * GeoTools 快速入门示例应用程序。
     * 提示用户选择一个 shapefile 文件，并在地图框架中显示其内容
     * @param args 忽略，不需要参数
     * @throws Exception 如果在查找 shapefile 时出现错误
     */
    public static void main(String[] args) throws Exception {
        // 显示一个用于选择 shapefile 的数据存储文件选择对话框
        LOGGER.info("快速入门");
        LOGGER.config("欢迎开发者");
        LOGGER.info("java.util.logging.config.file=" + System.getProperty("java.util.logging.config.file"));

        // 打开文件选择对话框，让用户选择 .shp 格式的文件
        File file = JFileDataStoreChooser.showOpenFile("shp", null);
        if (file == null) {
            // 如果用户取消选择或没有选择文件，直接返回
            return;
        }
        LOGGER.config("已选择文件：" + file);

        // 通过选择的文件获取文件数据存储对象
        FileDataStore store = FileDataStoreFinder.getDataStore(file);
        // 从数据存储中获取简单要素源（包含空间数据）
        SimpleFeatureSource featureSource = store.getFeatureSource();

        // 创建地图内容对象，并将 shapefile 添加到其中
        MapContent map = new MapContent();
        map.setTitle("快速入门");

        // 根据要素源的模式（schema）创建一个简单样式
        // 样式定义了要素在地图上的显示方式（颜色、线宽等）
        Style style = SLD.createSimpleStyle(featureSource.getSchema());

        // 创建要素图层，将要素源和样式结合
        FeatureLayer layer = new FeatureLayer(featureSource, style);

        // 将图层添加到地图内容中
        map.addLayer(layer);

        // 在窗口中显示地图
        JMapFrame.showMap(map);
    }
}
