package com.zhangyh.shapefile.shapefile;

import org.geotools.api.data.*;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.text.cql2.CQL;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Shapefile 操作教学示例类
 * 包含：读取、创建、过滤、空间分析（缓冲区）等操作
 *
 * @author zhangyh
 */
public class ShapeFileOperator {

    private static final Logger LOGGER = Logger.getLogger(ShapeFileOperator.class.getName());
    private static final GeometryFactory geometryFactory = new GeometryFactory();

    public static void main(String[] args) {
        try {
            // 示例文件路径 (请确保该文件存在，或者使用 createShapefile 创建一个新的)
            String shapefilePath = "src/main/resources/templates/shapefile1/229_prescription.shp";
            File file = new File(shapefilePath);

            // 1. 读取 Shapefile 信息
            if (file.exists()) {
                readShapefileInfo(file);
            } else {
                LOGGER.warning("文件不存在: " + file.getAbsolutePath());
            }

            // 2. 创建一个新的点 Shapefile
            File newShapefile = new File("new_points.shp");
            createPointShapefile(newShapefile);
            readShapefileInfo(newShapefile); // 验证创建结果

            // 3. 过滤查询 (示例)
            // 注意：这里假设 new_points.shp 中有 'name' 字段
            filterFeatures(newShapefile, "name = 'Point 1'");

            // 4. 缓冲区分析 (Buffer)
            // 对新创建的点数据进行缓冲区分析，生成面数据
            bufferFeatures(newShapefile, 10.0); // 缓冲距离 10 单位

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 1. 读取 Shapefile 的元数据和要素
     * 原理：Shapefile 由 .shp (几何), .dbf (属性), .shx (索引) 等文件组成。
     * GeoTools 使用 DataStore 接口来抽象数据源。
     */
    public static void readShapefileInfo(File file) throws Exception {
        System.out.println("========== 开始读取 Shapefile: " + file.getName() + " ==========");
        
        // DataStore 是连接物理存储的桥梁
        FileDataStore store = FileDataStoreFinder.getDataStore(file);
        
        // FeatureSource 用于读取要素
        SimpleFeatureSource featureSource = store.getFeatureSource();
        
        // FeatureType 描述了数据的结构（Schema），包括字段名、类型、坐标系等
        SimpleFeatureType schema = featureSource.getSchema();
        System.out.println("图层名称: " + schema.getTypeName());
        System.out.println("几何类型: " + schema.getGeometryDescriptor().getType().getBinding().getSimpleName());
        System.out.println("坐标参考系 (CRS): " + schema.getCoordinateReferenceSystem());
        System.out.println("字段列表:");
        for (int i = 0; i < schema.getAttributeCount(); i++) {
            System.out.println("\t" + schema.getDescriptor(i).getLocalName() + " (" + schema.getDescriptor(i).getType().getBinding().getSimpleName() + ")");
        }

        // 获取要素数量
        int count = featureSource.getCount(Query.ALL);
        System.out.println("要素总数: " + count);

        // 遍历要素 (使用 try-with-resources 确保迭代器关闭)
        // 注意：使用 store.getFeatureReader 时，必须在 Query 中指定 TypeName，否则会报错 "Query does not specify type"
        Query query = new Query(schema.getTypeName());
        try (FeatureReader<SimpleFeatureType, SimpleFeature> reader = store.getFeatureReader(query, Transaction.AUTO_COMMIT)) {
            int printCount = 0;
            while (reader.hasNext() && printCount < 5) { // 只打印前5条
                SimpleFeature feature = reader.next();
                System.out.println("要素ID: " + feature.getID() + " | 属性: " + feature.getAttributes());
                printCount++;
            }
        }
        store.dispose(); // 释放资源
        System.out.println("========== 读取结束 ==========\n");
    }

    /**
     * 2. 创建一个新的 Shapefile (点图层)
     * 原理：
     * 1. 定义 SimpleFeatureType (Schema)。
     * 2. 创建 FeatureCollection 存放数据。
     * 3. 使用 DataStoreFactory 创建新的 Shapefile 文件。
     * 4. 将数据写入 DataStore。
     */
    public static void createPointShapefile(File file) throws Exception {
        System.out.println("========== 创建 Shapefile: " + file.getName() + " ==========");

        // 定义 Schema: "the_geom:Point:srid=4326,name:String,id:Integer"
        // srid=4326 代表 WGS84 经纬度坐标系
        final SimpleFeatureType TYPE = DataUtilities.createType("Location",
                "the_geom:Point:srid=4326," + // 几何字段，必须存在
                        "name:String," +              // 字符串属性
                        "number:Integer"              // 整数属性
        );

        // 创建要素集合
        List<SimpleFeature> features = new ArrayList<>();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(TYPE);

        // 添加第一个点
        builder.add(geometryFactory.createPoint(new Coordinate(116.397, 39.908))); // 北京
        builder.add("Beijing");
        builder.add(1);
        features.add(builder.buildFeature(null));

        // 添加第二个点
        builder.add(geometryFactory.createPoint(new Coordinate(121.473, 31.230))); // 上海
        builder.add("Shanghai");
        builder.add(2);
        features.add(builder.buildFeature(null));

        // 创建 Shapefile DataStore
        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
        Map<String, Serializable> params = new HashMap<>();
        params.put("url", file.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);

        ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
        newDataStore.setCharset(StandardCharsets.UTF_8); // 设置编码，防止中文乱码

        // 在 DataStore 中创建 Schema
        newDataStore.createSchema(TYPE);

        // 写入数据
        Transaction transaction = new DefaultTransaction("create");
        String typeName = newDataStore.getTypeNames()[0];
        SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

        if (featureSource instanceof SimpleFeatureStore) {
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
            featureStore.setTransaction(transaction);
            try {
                featureStore.addFeatures(DataUtilities.collection(features));
                transaction.commit();
                System.out.println("成功写入 " + features.size() + " 个要素。");
            } catch (Exception problem) {
                problem.printStackTrace();
                transaction.rollback();
            } finally {
                transaction.close();
            }
        } else {
            System.out.println(typeName + " 不支持写入");
        }
        System.out.println("========== 创建结束 ==========\n");
    }

    /**
     * 3. 过滤查询
     * 原理：使用 CQL (Common Query Language) 构建过滤器，筛选符合条件的要素。
     */
    public static void filterFeatures(File file, String cqlQuery) throws Exception {
        System.out.println("========== 过滤查询: " + cqlQuery + " ==========");
        FileDataStore store = FileDataStoreFinder.getDataStore(file);
        SimpleFeatureSource featureSource = store.getFeatureSource();

        // 创建 Filter
        Filter filter = CQL.toFilter(cqlQuery);

        // 使用 Filter 获取 FeatureCollection
        SimpleFeatureCollection collection = featureSource.getFeatures(filter);
        
        System.out.println("满足条件的要素数量: " + collection.size());
        try (SimpleFeatureIterator iterator = collection.features()) {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                System.out.println("匹配要素: " + feature.getAttribute("name"));
            }
        }
        store.dispose();
        System.out.println("========== 过滤结束 ==========\n");
    }

    /**
     * 4. 缓冲区分析 (Buffer)
     * 原理：读取要素几何，使用 JTS (Java Topology Suite) 进行 buffer 操作，生成新的几何，保存为新文件。
     */
    public static void bufferFeatures(File inputFile, double distance) throws Exception {
        System.out.println("========== 缓冲区分析 (距离: " + distance + ") ==========");
        
        FileDataStore inputStore = FileDataStoreFinder.getDataStore(inputFile);
        SimpleFeatureSource inputSource = inputStore.getFeatureSource();
        SimpleFeatureType inputSchema = inputSource.getSchema();

        // 定义输出 Schema (将 Point 改为 Polygon，因为 buffer 结果是面)
        SimpleFeatureType outputSchema = DataUtilities.createType("Buffer",
                "the_geom:Polygon:srid=4326," +
                        "original_name:String"
        );

        List<SimpleFeature> outputFeatures = new ArrayList<>();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(outputSchema);

        // 遍历输入要素
        SimpleFeatureCollection inputCollection = inputSource.getFeatures();
        try (SimpleFeatureIterator iterator = inputCollection.features()) {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                Geometry geom = (Geometry) feature.getDefaultGeometry();
                String name = (String) feature.getAttribute("name");

                // 执行 Buffer 操作
                Geometry bufferGeom = geom.buffer(distance);

                // 构建新要素
                builder.add(bufferGeom);
                builder.add(name);
                outputFeatures.add(builder.buildFeature(null));
            }
        }

        // 保存到新文件
        File outputFile = new File("buffered_result.shp");
        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
        Map<String, Serializable> params = new HashMap<>();
        params.put("url", outputFile.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);

        ShapefileDataStore outputStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
        outputStore.createSchema(outputSchema);
        
        Transaction transaction = new DefaultTransaction("buffer");
        SimpleFeatureStore featureStore = (SimpleFeatureStore) outputStore.getFeatureSource(outputStore.getTypeNames()[0]);
        featureStore.setTransaction(transaction);
        try {
            featureStore.addFeatures(DataUtilities.collection(outputFeatures));
            transaction.commit();
            System.out.println("缓冲区生成成功: " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            transaction.rollback();
            e.printStackTrace();
        } finally {
            transaction.close();
            inputStore.dispose();
        }
        System.out.println("========== 分析结束 ==========\n");
    }
}
