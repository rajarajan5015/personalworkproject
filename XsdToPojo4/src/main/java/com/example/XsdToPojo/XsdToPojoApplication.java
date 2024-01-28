package com.example.XsdToPojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.apache.avro.Schema;
import org.apache.avro.reflect.ReflectData;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaFacet;
import org.apache.ws.commons.schema.XmlSchemaMaxLengthFacet;
import org.apache.ws.commons.schema.XmlSchemaMinLengthFacet;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.ausiex.avro.cef.Message;


@SpringBootApplication
public class XsdToPojoApplication {
	private static XmlSchemaCollection xmlSchemaCollection;
	private static Map<QName, List<XmlSchemaElement>> xsdElements = new HashMap<QName, List<XmlSchemaElement>>();
	static Map<String, Map<String, String>> resultMap = new HashMap<>();

	public static void main(String[] args) throws Exception {
		SpringApplication.run(XsdToPojoApplication.class, args);
		Schema xsdschema = ReflectData.get().getSchema(Message.class);
		String data = "src/main/resources/AvroSchema/OutputAvroFile.avsc";
		File schemaFile = new File(data);
		FileWriter fileWriter = new FileWriter(schemaFile);
		fileWriter.append(xsdschema.toString(true));
		fileWriter.close();

		// Path for the file which is stored within the Resource folder
		String xsdPath = "src/main/resources/xsd/cashmovement.xsd";
		InputStream is = new FileInputStream(xsdPath);
		xmlSchemaCollection = new XmlSchemaCollection();

		// Schema contain the complete XSD content which needs to be parsed
		XmlSchema schema = xmlSchemaCollection.read(new StreamSource(is));

		// Get the root element from XSD
		Map.Entry<QName, XmlSchemaElement> entry = schema.getElements().entrySet().iterator().next();
		for (Entry<QName, XmlSchemaElement> ss : schema.getElements().entrySet()) {

			getChildElementNames(ss.getValue(), resultMap);
		}

		try {

			BufferedReader reader = new BufferedReader(new FileReader(schemaFile));

			StringBuilder schemaBuilder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				schemaBuilder.append(line).append("\n");
			}
			reader.close();

			// Update the schema using regex
			String schema1 = schemaBuilder.toString();
			String updatedSchema = schema1.replaceAll("name(.*)", "name$1\"doc\" : \"\",");

			Schema.Parser mandatoryparser = new Schema.Parser();
			Schema mandatoryschema = mandatoryparser.parse(updatedSchema);

			mapper(mandatoryschema);

			FileWriter fileWriter2 = new FileWriter(schemaFile);
			fileWriter2.append(mandatoryschema.toString(true));
			fileWriter2.close();
			System.out.println(
					mandatoryschema.toString(true) + "\n" + "***OutputData File Created in concerned avsc file***");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static void mapper(Schema mandatoryschema) {
		for (Schema.Field field : mandatoryschema.getFields()) {
			if (field.schema().getType().getName().toString().equalsIgnoreCase("RECORD")) {
				mapper(field.schema());
			}
			for (Entry<String, Map<String, String>> map : resultMap.entrySet()) {
				if (field.name().toString().equalsIgnoreCase(map.getKey().toString())) {
					if (map.getValue().get("minLength") != null) {
						int minLength = Integer.parseInt(map.getValue().get("minLength"));
						field.addProp("minLength", minLength);
					}
					if (map.getValue().get("maxLength") != null) {
						int maxLength = Integer.parseInt(map.getValue().get("maxLength"));
						field.addProp("maxLength", maxLength);
					}
					break;
				}
			}
		}
	}

	private static void getChildElementNames(XmlSchemaElement element, Map<String, Map<String, String>> resultMap) {

		// Get the type of the element
		XmlSchemaType elementType = element != null ? element.getSchemaType() : null;

		if (elementType instanceof XmlSchemaSimpleType) {
			// Get all particles associated with that element Type
			List<XmlSchemaFacet> allParticles = ((XmlSchemaSimpleTypeRestriction) ((XmlSchemaSimpleType) elementType)
					.getContent()).getFacets();
			Map<String, String> elementInfoMap = new HashMap<>();

			for (XmlSchemaFacet XmlSchemaFacet : allParticles) {
				if (XmlSchemaFacet instanceof XmlSchemaMaxLengthFacet) {
					elementInfoMap.put("maxLength", ((XmlSchemaMaxLengthFacet) XmlSchemaFacet).getValue().toString());
				}
				if (XmlSchemaFacet instanceof XmlSchemaMinLengthFacet) {
					elementInfoMap.put("minLength", ((XmlSchemaMinLengthFacet) XmlSchemaFacet).getValue().toString());
				}
			}

			resultMap.put(element.getName(), elementInfoMap);

		} else {
		}

	}

	// Add child elements based on its parent
	public static void addChild(QName qName, XmlSchemaElement child) {
		List<XmlSchemaElement> values = xsdElements.get(qName);
		if (values == null) {
			values = new ArrayList<XmlSchemaElement>();
		}
		values.add(child);
		xsdElements.put(qName, values);
	}
}

