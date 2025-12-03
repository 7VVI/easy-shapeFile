package com.zhangyh.shapefile.shapefile;

import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.style.Style;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.SLD;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.data.JFileDataStoreChooser;

import java.io.File;

/**
 * @author zhangyh
 * @Date 2025/12/3 13:51
 * @desc
 */
public class ShapeFileOpenInner {
    public static void main(String[] args) throws Exception {
        // display a data store file chooser dialog for shapefiles
        File file = JFileDataStoreChooser.showOpenFile("shp", null);
        if (file == null) {
            return;
        }

        FileDataStore store = FileDataStoreFinder.getDataStore(file);
        SimpleFeatureSource featureSource = store.getFeatureSource();
        
        // 调试信息：打印要素数量和边界
        System.out.println("正在打开文件: " + file.getAbsolutePath());
        System.out.println("要素类型: " + featureSource.getSchema().getTypeName());
        System.out.println("几何类型: " + featureSource.getSchema().getGeometryDescriptor().getType().getBinding().getSimpleName());
        System.out.println("坐标系: " + featureSource.getSchema().getCoordinateReferenceSystem());
        System.out.println("要素数量: " + featureSource.getFeatures().size());
        System.out.println("边界范围: " + featureSource.getBounds());

        // 直接使用 featureSource，避免使用 SpatialIndexFeatureCollection 导致空指针问题
        // SimpleFeatureSource cachedSource =
        //        DataUtilities.source(
        //                new SpatialIndexFeatureCollection(featureSource.getFeatures()));

        // Create a map content and add our shapefile to it
        MapContent map = new MapContent();
        map.setTitle("Using cached features");
        
        // 创建样式
        // 注意：SLD.createSimpleStyle 可能会生成透明或不可见的默认样式，特别是对于 Polygon
        // 这里我们尝试显式创建一个 Polygon 样式，确保有边框颜色
        Style style = SLD.createPolygonStyle(java.awt.Color.RED, java.awt.Color.BLUE, 0.5f);
        // 如果不是 Polygon，尝试用通用的 createSimpleStyle
        if (style == null) {
             style = SLD.createSimpleStyle(featureSource.getSchema());
        }
        
        // 使用原始 featureSource 创建图层
        Layer layer = new FeatureLayer(featureSource, style);
        map.addLayer(layer);

        // Now display the map
        JMapFrame.showMap(map);
    }
}
