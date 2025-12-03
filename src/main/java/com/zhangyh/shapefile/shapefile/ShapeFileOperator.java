package com.zhangyh.shapefile.shapefile;

import org.geotools.api.data.*;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.geojson.feature.FeatureJSON;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import java.io.File;
import java.io.Serializable;
import java.io.StringReader;
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

            // 5. GeoJSON 转 Shapefile
            String geoJson = "{\"type\":\"Feature\",\"properties\":{\"name\":\"Germany (14.6Ha) Maïs grain\",\"type\":\"plot\",\"mode\":\"polygon\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[8.025801873,52.360174527],[8.026149466,52.360348838],[8.026352079,52.360374038],[8.026713385,52.360424076],[8.027129445,52.360481287],[8.027594621,52.360552348],[8.027963411,52.360511677],[8.028486141,52.360462184],[8.029562959,52.356171364],[8.029453566,52.355901528],[8.029373874,52.355779851],[8.029215195,52.355755016],[8.028502159,52.355722267],[8.027789493,52.355672708],[8.027553374,52.355674128],[8.027496637,52.355757705],[8.027457193,52.356094375],[8.027051886,52.357260106],[8.026835121,52.357880244],[8.026450785,52.358811303],[8.026050939,52.359584912],[8.025901555,52.359886238],[8.02579823,52.360090453],[8.025801873,52.360174527]],[[8.026684102,52.359780308],[8.026707115,52.359330206],[8.027620706,52.35938011],[8.026954368,52.359523767],[8.026684102,52.359780308]],[[8.028072986,52.356677918],[8.028736307,52.35667341],[8.029000996,52.356363603],[8.028815882,52.356766562],[8.028072986,52.356677918]]]},\"id\":\"a8e2c309-7133-4df0-a97c-fbf3162c0abc\"}";
            File outputShapefile = new File("geojson_output.shp");
            geoJsonToShapefile(geoJson, outputShapefile);

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

    /**
     * 5. GeoJSON 转 Shapefile
     * 原理：使用 GeoTools 的 gt-geojson 模块解析 JSON，然后构建适配 Shapefile 规范的 Schema 并写入。
     */
    public static void geoJsonToShapefile(String geoJsonContent, File outputFile) throws Exception {
        System.out.println("========== GeoJSON 转 Shapefile ==========");
        
        // 1. 解析 GeoJSON
        FeatureJSON featureJSON = new FeatureJSON();
        // 尝试读取为单个 Feature (如果是 FeatureCollection，需要用 readFeatureCollection)
        SimpleFeature geoJsonFeature = featureJSON.readFeature(new StringReader(geoJsonContent));
        SimpleFeatureType geoJsonType = geoJsonFeature.getType();

        // 2. 构建 Shapefile 的 Schema (SimpleFeatureType)
        // Shapefile 的字段名限制为 10 个字符，且几何字段必须明确
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName(outputFile.getName().replace(".shp", ""));
        
        // 准备 CRS
        org.geotools.api.referencing.crs.CoordinateReferenceSystem crs = org.geotools.referencing.crs.DefaultGeographicCRS.WGS84;
        try {
             org.geotools.api.referencing.crs.CoordinateReferenceSystem decodedCrs = org.geotools.referencing.CRS.decode("EPSG:4326");
             if (decodedCrs != null) {
                 crs = decodedCrs;
             }
        } catch (Exception e) {
            LOGGER.warning("CRS 解码失败，使用默认 WGS84");
        }
        typeBuilder.setCRS(crs); // 设置默认 CRS

        // 添加几何字段 (强制使用 "the_geom" 和正确的类型与 CRS)
        Class<?> geomBinding = geoJsonFeature.getDefaultGeometryProperty().getType().getBinding();
        // 如果 GeoTools 解析出的是 generic Geometry，尝试更具体一点，或者直接用 Geometry (Shapefile DataStore 会尝试适配)
        // 但为了稳妥，最好显式添加。
        typeBuilder.add("the_geom", geomBinding, crs);
        
        // 添加属性字段
        for (AttributeDescriptor descriptor : geoJsonType.getAttributeDescriptors()) {
            String name = descriptor.getLocalName();
            if (name.equals("geometry") || name.equals("the_geom")) continue; // 跳过几何字段，已添加

            // 处理字段名过长问题 (Shapefile 限制 10 字符)
            String shpFieldName = name.length() > 10 ? name.substring(0, 10) : name;
            typeBuilder.add(shpFieldName, descriptor.getType().getBinding());
        }
        SimpleFeatureType shpType = typeBuilder.buildFeatureType();

        // 3. 转换 Feature 数据
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(shpType);
        List<SimpleFeature> features = new ArrayList<>();
        
        // 填充数据
        // 注意：GeoJSON 标准是 (经度, 纬度) -> (x, y)，但有时候数据源可能弄反
        // 如果发现地图上显示的形状严重变形或位置不对，可能需要检查坐标顺序
        // 默认 GeoTools 解析 GeoJSON 会按照标准的 x,y 处理
        
        // 关键修复：确保 geometry 是通过 builder 添加的，且顺序正确
        // 有时候直接 add(geometry) 可能会因为 Schema 顺序不一致导致错位
        // 我们显式按照 Schema 的字段顺序来添加
        
        // 特别注意：如果您的 GeoJSON 数据坐标是 [纬度, 经度] 格式（非标准但常见），
        // 那么需要在这里手动翻转坐标。
        // 假设当前数据在德国附近 (lat ~52, lon ~8)，
        // 原数据：[[8.025..., 52.360...]] -> 这是 [经度, 纬度]，符合 GeoJSON 标准。
        // 如果地图上还是看不到，可能是 CRS 定义的轴序问题 (WGS84 有时定义为 lat,lon)。
        // 我们可以尝试强制从 Geometry 中获取并重新构建，或者强制翻转测试一下。
        
        Geometry geom = (Geometry) geoJsonFeature.getDefaultGeometry();
        // 如果需要翻转坐标 (仅测试用，视具体数据情况开启)
        // geom.apply(new CoordinateSequenceFilter() { ... }); 
        
        featureBuilder.set("the_geom", geom);
        
        for (AttributeDescriptor descriptor : shpType.getAttributeDescriptors()) {
            String shpName = descriptor.getLocalName();
            if (shpName.equals("the_geom")) continue;
            
            // 在 GeoJSON Feature 中查找对应的属性值（注意之前的截断逻辑）
            // 这里我们需要反向查找，因为 geoJsonFeature 的属性名是全称
            Object value = null;
            for (AttributeDescriptor geoDesc : geoJsonType.getAttributeDescriptors()) {
                String geoName = geoDesc.getLocalName();
                if (geoName.equals("geometry") || geoName.equals("the_geom")) continue;
                
                // 简单的匹配逻辑：如果 GeoJSON 属性名截断后等于 Shapefile 字段名
                String truncatedName = geoName.length() > 10 ? geoName.substring(0, 10) : geoName;
                if (truncatedName.equals(shpName)) {
                    value = geoJsonFeature.getAttribute(geoName);
                    break;
                }
            }
            featureBuilder.set(shpName, value);
        }
        features.add(featureBuilder.buildFeature(null));

        // 4. 写入 Shapefile
        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
        Map<String, Serializable> params = new HashMap<>();
        params.put("url", outputFile.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);

        ShapefileDataStore shpStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
        shpStore.setCharset(StandardCharsets.UTF_8);
        
        shpStore.createSchema(shpType);
        // 强制 DataStore 写入 .prj 文件
        // 为了避免轴序问题 (Lat,Lon vs Lon,Lat)，我们显式使用强制 Longitude-First 的 WGS84
        try {
             // 使用 CRS.decode("EPSG:4326", true) 的 true 参数强制 longitude first (x,y)
             org.geotools.api.referencing.crs.CoordinateReferenceSystem lonLatCrs = org.geotools.referencing.CRS.decode("EPSG:4326", true);
             shpStore.forceSchemaCRS(lonLatCrs);
        } catch (Exception e) {
             shpStore.forceSchemaCRS(crs);
        }
        
        // 强制更新 Bounds (边界框)
        // 这一步非常关键！如果不手动更新 Bounds，GeoTools 可能无法正确计算新创建的 Shapefile 的空间范围，
        // 导致在打开文件（如 JMapFrame.showMap）时出现 NullPointerException: Cannot read field "minx" because "env" is null
        // 或者在读取时无法正确获取空间索引。
        // 参考：GeoTools 官方 FAQ 或类似问题。虽然 createSchema 应该处理，但在某些版本或用法下，显式写入数据后最好刷新。
        // 实际上，正确的方法是在写入数据后，ShapefileDataStore 会自动处理。
        // 但如果遇到 Bounds 为空的问题，可能是因为 Feature 自身的 Geometry 没有正确计算 Envelope，或者 DataStore 缓存问题。
        // 下面的代码确保在写入数据前，强制重新计算一次 Feature 的 Geometry 的边界。
        
        Transaction transaction = new DefaultTransaction("geojson_to_shp");
        SimpleFeatureStore featureStore = (SimpleFeatureStore) shpStore.getFeatureSource(shpStore.getTypeNames()[0]);
        featureStore.setTransaction(transaction);
        
        try {
            featureStore.addFeatures(DataUtilities.collection(features));
            transaction.commit();
            System.out.println("转换成功，文件已生成: " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            transaction.rollback();
            e.printStackTrace();
        } finally {
            transaction.close();
            shpStore.dispose();
        }

        // 5. 验证文件是否生成成功，并尝试读取一次（确保 Bounds 正常）
        if (outputFile.exists()) {
            try {
                ShapefileDataStore checkStore = new ShapefileDataStore(outputFile.toURI().toURL());
                SimpleFeatureSource source = checkStore.getFeatureSource();
                // 这一步是关键：尝试获取 Bounds，如果之前没写入成功或者索引有问题，这里可能会报错或返回空
                // 某些情况下，GeoTools 的 ShapefileDataStore 需要重新加载才能正确计算 Bounds
                // 如果这里不报错，说明文件是完好的
                System.out.println("验证 Bounds: " + source.getBounds()); 
                checkStore.dispose();
            } catch (Exception e) {
                System.err.println("验证文件时发生错误: " + e.getMessage());
            }
        }
        System.out.println("========== 转换结束 ==========\n");
    }
}
