package octopus.utils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

public class ResultSetJsonBuilder extends DaoBase {
	private Method openMethod;

	public static abstract class FieldSerializer {
		public abstract void serialize(JsonGenerator json, FieldConfig fieldCfg, Object fieldValue)
				throws JsonGenerationException, IOException;
	}

	public class FieldConfig {
		public Field field;
		public String fieldName;
		public boolean visible = true;
		public FieldSerializer serializer = null;
		public int tag = 0;

		public FieldConfig(Field dataField) {
			super();
			dataField.setAccessible(true);
			this.field = dataField;
		}
	}

	public FieldConfig[] fieldConfigs = null;

	/***
	 * This method is for calling the open method in a reflection way
	 * 
	 * @return
	 */
	public boolean openByReflection(Object... args) {
		if (openMethod != null)
			try {
				openMethod.invoke(this, args);
				return (boolean) openMethod.invoke(this, args);
			} catch (InvocationTargetException e) {
				Throwable targetException = e.getTargetException();
				if (targetException instanceof RuntimeException)
					throw (RuntimeException) targetException;
				else {
					targetException.printStackTrace();
					throw new RuntimeException("Open method calling failed!");
				}
			} catch (IllegalAccessException | IllegalArgumentException e) {
				e.printStackTrace();
				throw new RuntimeException("Open method calling failed!");
			}
		return false;
	}

	public ResultSetJsonBuilder() {
		openMethod = null;
		for (Method aMethod : getClass().getMethods())
			if (aMethod.getName().equals("open")) {
				openMethod = aMethod;
				break;
			}
		List<Field> fieldList = new ArrayList<Field>();
		Field[] fields = getClass().getFields();
		for (Field field : fields) {
			if (field.getName().startsWith("f_")) {
				field.setAccessible(true);
				fieldList.add(field);
			}
		}
		fieldConfigs = new FieldConfig[fieldList.size()];
		for (int i = 0; i < fieldConfigs.length; i++) {
			FieldConfig aFieldCfg = new FieldConfig(fieldList.get(i));
			fieldConfigs[i] = aFieldCfg;
			String defaultName = aFieldCfg.field.getName().substring(2);
			defaultName = defaultName.substring(0, 1).toLowerCase() + defaultName.substring(1);
			aFieldCfg.fieldName = defaultName;
			aFieldCfg.visible = true;
			onFieldSetup(i, aFieldCfg);
		}
	}

	public void clearFields() {
		try {
			for (int i = 0; i < fieldConfigs.length; i++)
				fieldConfigs[i].field.set(this, null);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public void onFieldSetup(int idx, FieldConfig fieldCfg) {

	}
}
