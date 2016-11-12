package factory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import resource.Resource;
import test.Autowired;
import test.Component;
import bean.BeanDefinition;
import bean.BeanUtil;
import bean.PropertyValue;
import bean.PropertyValues;

public class ClassPathXmlApplicationContext extends AbApplicationContext{	

	String root = System.getProperty("user.dir") + File.separator+ "src";
	
	public ClassPathXmlApplicationContext(Resource resource)
	{		
		ConfigManager(root);	
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dbBuilder = dbFactory.newDocumentBuilder();
			Document document = dbBuilder.parse(resource.getInputStream());
            NodeList beanList = document.getElementsByTagName("bean");
            
            for(int i = 0 ; i < beanList.getLength(); i++)
            {
            	Node bean = beanList.item(i);
            	BeanDefinition beandef = new BeanDefinition();
            	String beanClassName = bean.getAttributes().getNamedItem("class").getNodeValue();
            	String beanName = bean.getAttributes().getNamedItem("id").getNodeValue();
            	
        		beandef.setBeanClassName(beanClassName);
        		
				try {
					Class<?> beanClass = Class.forName(beanClassName);
					beandef.setBeanClass(beanClass);
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        		
        		PropertyValues propertyValues = new PropertyValues();
        		
        		NodeList propertyList = bean.getChildNodes();
            	for(int j = 0 ; j < propertyList.getLength(); j++)
            	{
            		Node property = propertyList.item(j);
            		if (property instanceof Element) {
        				Element ele = (Element) property;
        				
        				String name = ele.getAttribute("name");
        				Class<?> type;
        				
        				if(!ele.getAttribute("ref").isEmpty()){
        					String ref=ele.getAttribute("ref");
        					
        					if(isBeanExist(ref)){
        						propertyValues.AddPropertyValue(new PropertyValue(name,this.getBean(ref)));
        					}else{
        						ref(ref,resource, i+1);
        						propertyValues.AddPropertyValue(new PropertyValue(name,this.getBean(ref)));
        					}
        				}else if(!ele.getAttribute("value").isEmpty()){						
        					try {						
        						type = beandef.getBeanClass().getDeclaredField(name).getType();								
        						Object value = ele.getAttribute("value");							     					        				
        						if(type == Integer.class)	        				
        						{        					
        							value = Integer.parseInt((String) value);      				
        						}else if(type == String.class){        					
        							value = String.valueOf((String) value);     			
        						}

        						propertyValues.AddPropertyValue(new PropertyValue(name,value));
					
        					} catch (NoSuchFieldException e) {
							// TODO Auto-generated catch block	
        						e.printStackTrace();				
        					} catch (SecurityException e) {
							// TODO Auto-generated catch block			
        						e.printStackTrace();		
        					}
        				
        				}
        				
        			}
            	}
            	beandef.setPropertyValues(propertyValues);

            	if(!this.isBeanExist(beanName)){     	
            		this.registerBeanDefinition(beanName, beandef);
            	}

            }
            
		} catch (Exception e) {
            e.printStackTrace();
		}
	}

	
	@Override
	protected BeanDefinition GetCreatedBean(BeanDefinition beanDefinition) {
		
		try {
			// set BeanClass for BeanDefinition
			
			Class<?> beanClass = beanDefinition.getBeanClass();
			// set Bean Instance for BeanDefinition
			Object bean = null;
			
			if(awd(beanClass)==null){
				bean=beanClass.newInstance();
				List<PropertyValue> fieldDefinitionList = beanDefinition.getPropertyValues().GetPropertyValues();
				for(PropertyValue propertyValue: fieldDefinitionList)
				{
					BeanUtil.invokeSetterMethod(bean, propertyValue.getName(), propertyValue.getValue());
				}
				
				beanDefinition.setBean(bean);			
				return beanDefinition;
			}else{
				bean=awd(beanClass);
				beanDefinition.setBean(bean);
				return beanDefinition;
			}
		
			
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return null;
	}
	
	public void ConfigManager(String path) {
		
		File readFile = new File(path);
		File[] files = readFile.listFiles();
		
		String fileName = null;
		for (File file : files) {
			fileName = file.getName();
			if (file.isFile()) {
				
				if (fileName.endsWith(".java")) {			
					try {
						
						String  str=path+File.separator+ fileName;
						String beanClassName=str.substring(root.length()+1, str.length()-5).replace('\\', '.');
						Class<?> beanClass = Class.forName(beanClassName);
					
						if(beanClass.isAnnotationPresent(Component.class)){
		
							Field[] filedList=beanClass.getDeclaredFields();
							BeanDefinition beandef = new BeanDefinition();
							PropertyValues propertyValues = new PropertyValues();						
							String beanName=beanClass.getAnnotation(Component.class).value();
							
							for(Field filed:filedList){
								filed.setAccessible(true); 		
								
								if(filed.getType().getName().equals(java.lang.String.class.getName())){				
									Object value=filed.get(beanClass.newInstance());						
									propertyValues.AddPropertyValue(new PropertyValue(filed.getName(),value));
								}	
							}
							beandef.setBeanClass(beanClass);
							beandef.setBeanClassName(beanClassName);
							beandef.setPropertyValues(propertyValues);	
							
							this.registerBeanDefinition(beanName, beandef);
						}			
						
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}  
				}
			} else {
				ConfigManager(path + File.separator + fileName);
			}
		}
	}
	
	public void  ref(String beanName,Resource resource,int i){
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();			
		DocumentBuilder builder;
		
		try {					
			builder = factory.newDocumentBuilder();				
			Document document = builder.parse(resource.getInputStream());		       
			NodeList list = document.getElementsByTagName("bean");
			
			for(int k = i ; k < list.getLength(); k++)
	            {
	            	Node bean = list.item(k);	 
	            	String beanName1 = bean.getAttributes().getNamedItem("id").getNodeValue();
	            	
	            	if(beanName.equals(beanName1)){	
	            		
	            		BeanDefinition beandefinition = new BeanDefinition();
		            	String beanClassName = bean.getAttributes().getNamedItem("class").getNodeValue();
	            		
		            	beandefinition.setBeanClassName(beanClassName);
	            		
		            	try{
							Class<?> beanClass = Class.forName(beanClassName);
							beandefinition.setBeanClass(beanClass);
						} catch (ClassNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
		            	
		            	PropertyValues propertyValues = new PropertyValues();
		            	
		            	NodeList propertyList = bean.getChildNodes();
		            	
		            	for(int j = 0 ; j < propertyList.getLength(); j++){	
		            		Node property = propertyList.item(j);
		            		if (property instanceof Element) {
		        				Element element = (Element) property;
		        				
		        				String name = element.getAttribute("name");
		        				Class<?> type;
		        				
		        				if(!element.getAttribute("ref").isEmpty()){
		        					String  ref = element.getAttribute("ref");
		        					if(isBeanExist(ref)){
		        						propertyValues.AddPropertyValue(new PropertyValue(name,this.getBean(ref)));
		        					}else{
		        						ref(ref,resource, j+1);
		        						propertyValues.AddPropertyValue(new PropertyValue(name,this.getBean(ref)));
		        					}
		        				}else if(!element.getAttribute("value").isEmpty()){	
								
		        					try {
									
		        						type = beandefinition.getBeanClass().getDeclaredField(name).getType();									
		        						Object value = element.getAttribute("value");							
		        						if(type == Integer.class)	        					        						
		        						{        					        							
		        							value = Integer.parseInt((String) value);      				        						
		        						}else if(type == String.class){        						        							
		        							value = String.valueOf((String) value);     				        					
		        						}	        					
		        						propertyValues.AddPropertyValue(new PropertyValue(name,value));			        											
		        					} catch (NoSuchFieldException e) {
							
									// TODO Auto-generated catch block
									e.printStackTrace();															
		        					} catch (SecurityException e) {
									// TODO Auto-generated catch block								
									e.printStackTrace();													
		        					}	        				
		        				}
		        			}
		            	}
	            		
		            	beandefinition.setPropertyValues(propertyValues);
		            	this.registerBeanDefinition(beanName, beandefinition);
	            	}
	            }		
		} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block			
			e.printStackTrace();	
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@SuppressWarnings("rawtypes")
	public Object awd(Class<?> beanClass){	
		
		Constructor[] list=beanClass.getConstructors();

		for(Constructor constructor: list){
			
			if(constructor.isAnnotationPresent(Autowired.class)){
				
				Class[] paramTypes=constructor.getParameterTypes();	
				Object[] params=new Object[paramTypes.length];
				
				for(int i=0;i<paramTypes.length;i++){
					
					String s=(String)paramTypes[i].getName().replace('.','/');
					String[] s1=s.split("/");
					String beanName=s1[s1.length-1];
					
					if(this.isBeanExist(beanName)){
						params[i]=this.getBean(beanName);
					}
				}
				
				try {
				    return constructor.newInstance(params);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}				
		}
		
		return null;	
	}
}
