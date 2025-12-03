package com.zhangyh.shapefile;

import org.geotools.api.data.*;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
class EasyShapeFileApplicationTests {

    @Test
    void testReadShapefileFeature() throws IOException {
//        File file = new File("E:\\temp\\fms\\Prescription_ShapeFile\\Rx\\229_prescription.shp");
        ClassPathResource resource = new ClassPathResource("templates/shapefile1/229_prescription.shp");
        File file = resource.getFile();
        Map<String, Object> map = new HashMap<>();
        map.put("url", file.toURI().toURL());
        DataStore dataStore = DataStoreFinder.getDataStore(map);
        String typeName = dataStore.getTypeNames()[0];
        FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);
        Filter filter = Filter.INCLUDE; // ECQL.toFilter("BBOX(THE_GEOM, 10,20,30,40)")
        FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);
        try (FeatureIterator<SimpleFeature> features = collection.features()) {
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                System.out.print(feature.getID());
                System.out.print(": ");
                System.out.println(feature.getDefaultGeometryProperty().getValue());
            }
        }
    }

    @Test
    void createShapefile() throws IOException, SchemaException {
        FileDataStoreFactorySpi factory = new ShapefileDataStoreFactory();
        File file = new File("my.shp");
        Map<String, ?> map = Collections.singletonMap("url", file.toURI().toURL());
        DataStore myData = factory.createNewDataStore(map);
        SimpleFeatureType featureType =
                DataUtilities.createType("my", "geom:Point,name:String,age:Integer,description:String");
        myData.createSchema(featureType);
    }

    @Test
    void readShapefile() throws IOException, SchemaException {
        File file = new File("temp/Location.shp");
        FileDataStore myData = FileDataStoreFinder.getDataStore(file);
        SimpleFeatureSource source = myData.getFeatureSource();
        SimpleFeatureType schema = source.getSchema();
        Query query = new Query(schema.getTypeName());
        query.setMaxFeatures(1);
        FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(query);
        try (FeatureIterator<SimpleFeature> features = collection.features()) {
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                System.out.println(feature.getID() + ": ");
                for (Property attribute : feature.getProperties()) {
                    System.out.println("\t" + attribute.getName() + ":" + attribute.getValue());
                }
            }
        }
    }


    @Test
    void testDbf() throws IOException {
        // 这是一个应该可以工作的示例（警告：我没有尝试编译它）。
// 该示例假设第一个字段具有字符数据类型，第二个字段具有数字数据类型：
        FileInputStream fis = new FileInputStream( "D:\\Users\\Downloads\\Prescription_ShapeFile_20251202_Test处方图 (2)\\Rx\\229_prescription.dbf" );
        DbaseFileReader dbfReader =  new DbaseFileReader(fis.getChannel(),false,  Charset.forName("ISO-8859-1"));
        while ( dbfReader.hasNext() ){
            final Object[] fields = dbfReader.readEntry();

            String field1 = (String) fields[0];
            Integer field2 = (Integer) fields[1];

            System.out.println("DBF field 1 value is: " + field1);
            System.out.println("DBF field 2 value is: " + field2);
        }
        dbfReader.close();
        fis.close();
    }


    /**
     * 生成ShapeFile文件
     * @throws IOException
     * @throws SchemaException
     */
    @Test
    void writeShapefile() throws IOException, SchemaException {

        GeometryFactory geometryFactory = new GeometryFactory();

        List<Coordinate> points = List.of(
                new Coordinate(120.1, 30.1),
                new Coordinate(120.2, 30.1),
                new Coordinate(120.2, 30.2),
                new Coordinate(120.1, 30.2),
                new Coordinate(120.1, 30.1)   // 必须闭合
        );
        LinearRing ring = geometryFactory.createLinearRing(points.toArray(new Coordinate[0]));
        Polygon polygon = geometryFactory.createPolygon(ring);

        // 2. 创建 shapefile schema
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("polygon");
        builder.setCRS(org.geotools.referencing.crs.DefaultGeographicCRS.WGS84); // WGS84
        builder.add("the_geom", Polygon.class);
        builder.add("ZONE", String.class);
        builder.add("DOSE_UNIT", String.class);
        builder.add("PRODUCT", String.class);
        builder.add("DOSE", Double.class);
        SimpleFeatureType TYPE = builder.buildFeatureType();

        // 3. 创建 shapefile
        File file = new File("shape/polygon.shp"); // TODO 修改
        Map<String, Object> params = new HashMap<>();
        params.put("url", file.toURI().toURL());
        params.put("create spatial index", true);

        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
        ShapefileDataStore dataStore =
                (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);

        dataStore.createSchema(TYPE);
        dataStore.setCharset(Charset.forName("UTF-8"));

        // 4. 写入 Feature
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
        featureBuilder.add(polygon);
        featureBuilder.add("1");
        featureBuilder.add("kg/ha");
        featureBuilder.add("Rice");
        featureBuilder.add("10.9");
        SimpleFeature feature = featureBuilder.buildFeature(null);

        Transaction tx = new DefaultTransaction("create");

        try (FeatureWriter<SimpleFeatureType, SimpleFeature> writer =
                     dataStore.getFeatureWriterAppend(TYPE.getTypeName(), tx)) {

            SimpleFeature toWrite = writer.next();
            toWrite.setAttributes(feature.getAttributes());
            writer.write();
            tx.commit();

        } catch (Exception ex) {
            tx.rollback();
            throw ex;

        } finally {
            tx.close();
        }

        System.out.println("Shapefile 创建完成: " + file.getAbsolutePath());
    }


    /**
     * 生成ShapeFile文件 与上一个不同的是TYPE的声明方式不一样
     * @throws IOException
     * @throws SchemaException
     */
    @Test
    void testShapefile2() throws IOException, SchemaException {
        final SimpleFeatureType TYPE = DataUtilities.createType(
                "Location",
                "the_geom:Point:srid=4326," +
                        "name:String," +
                        "number:Integer"
        );

        System.out.println("Schema = \n" + TYPE);

        // ---------------------------------------------------------
        // 2) 创建 Shapefile 数据源
        // ---------------------------------------------------------
        File file = new File("temp/Location.shp"); // TODO 修改输出路径
        if (file.exists()) file.delete();

        Map<String, Object> params = new HashMap<>();
        params.put("url", file.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);

        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
        ShapefileDataStore dataStore =
                (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);

        dataStore.createSchema(TYPE);
        dataStore.setCharset(Charset.forName("UTF-8"));

        // ---------------------------------------------------------
        // 3) 创建一个 Point 要素
        // ---------------------------------------------------------
        GeometryFactory geometryFactory = new GeometryFactory();
        Point point = geometryFactory.createPoint(new Coordinate(120.12345, 30.67890));

        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
        featureBuilder.add(point);
        featureBuilder.add("HelloWorld");
        featureBuilder.add(88);

        SimpleFeature feature = featureBuilder.buildFeature(null);

        // ---------------------------------------------------------
        // 4) 写入 Shapefile
        // ---------------------------------------------------------
        Transaction tx = new DefaultTransaction("create");

        try (FeatureWriter<SimpleFeatureType, SimpleFeature> writer =
                     dataStore.getFeatureWriterAppend(TYPE.getTypeName(), tx)) {

            SimpleFeature toWrite = writer.next();
            toWrite.setAttributes(feature.getAttributes());

            writer.write();
            tx.commit();

        } catch (Exception e) {
            tx.rollback();
            throw e;

        } finally {
            tx.close();
        }

        System.out.println("Shapefile 创建成功: " + file.getAbsolutePath());
    }

}
