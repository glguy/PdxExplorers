package com.gmail.emertens.PdxExplorers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;

public class YmlDataFile {

	private static final String FILE_ENCODING = "UTF8";
	private final File file;
	private final DumperOptions options = new DumperOptions();

	public YmlDataFile(File file) {
		this.file = file;
		options.setDefaultFlowStyle(FlowStyle.BLOCK);
		options.setAllowUnicode(true);
	}

	public void save(Object o) throws IOException {
		if (file.isFile()) {
			file.delete();
		}

		FileOutputStream writer = new FileOutputStream(file);
		OutputStreamWriter osw = new OutputStreamWriter(writer, FILE_ENCODING);
		Yaml yaml = new Yaml(options);
		yaml.dump(o, osw); 
		osw.close();
	}

	public Object load() {
		Yaml yaml = new Yaml(options);
		Object result;
		try {
			FileInputStream reader = new FileInputStream(file);
			InputStreamReader isr = new InputStreamReader(reader, FILE_ENCODING);
			result = yaml.load(isr);
			isr.close();
		} catch (IOException e) {
			result = null;
		}

		return result;
	}
}
