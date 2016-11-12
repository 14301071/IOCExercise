package factory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import resource.Resource;
import test.Autowired;
import test.Component;
import bean.BeanDefinition;
import bean.PropertyValue;
import bean.PropertyValues;

public class ConfigManager extends AbApplicationContext{
	
	public void getConfig(String path) {
		
		File readFile = new File(path);
		File[] files = readFile.listFiles();
		
		String fileName = null;
		for (File file : files) {
			fileName = file.getName();
			if (file.isFile()) {
				
				if (fileName.endsWith(".java")) {			
					try {
						
						String  str=path+File.separator+ fileName;
						String beanClassName=str.substring(path.length()+1, str.length()-5).replace('\\', '.');
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
				getConfig(path + File.separator + fileName);
			}
		}
	}
	
	public void  ref(String beanName,Resource resource,int i){			
		try {						
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();			
			DocumentBuilder dbBuilder;				
			dbBuilder = dbFactory.newDocumentBuilder();				
			Document document = dbBuilder.parse(resource.getInputStream());		       
			NodeList beanList = document.getElementsByTagName("bean");
			
			for(int k = i ; k < beanList.getLength(); k++)
	            {
	            	Node bean = beanList.item(k);	 
	            	String beanName2 = bean.getAttributes().getNamedItem("id").getNodeValue();
	            	
	            	if(beanName.equals(beanName2)){	
	            		
	            		BeanDefinition beandef = new BeanDefinition();
		            	String beanClassName = bean.getAttributes().getNamedItem("class").getNodeValue();
	            		
		            	beandef.setBeanClassName(beanClassName);
	            		
		            	try{
							Class<?> beanClass = Class.forName(beanClassName);
							beandef.setBeanClass(beanClass);
						} catch (ClassNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
		            	
		            	PropertyValues propertyValues = new PropertyValues();
		            	
		            	NodeList propertyList = bean.getChildNodes();
		            	
		            	for(int j = 0 ; j < propertyList.getLength(); j++){	
		            		Node property = propertyList.item(j);
		            		if (property instanceof Element) {
		        				Element ele = (Element) property;
		        				
		        				String name = ele.getAttribute("name");
		        				Class<?> type;
		        				
		        				if(!ele.getAttribute("ref").isEmpty()){
		        					String  ref=ele.getAttribute("ref");
		        					if(isBeanExist(ref)){
		        						propertyValues.AddPropertyValue(new PropertyValue(name,this.getBean(ref)));
		        					}else{
		        						ref(ref,resource, j+1);
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
		            	this.registerBeanDefinition(beanName, beandef);
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
	
	public Object Awd(Class<?> beanClass){	
		Constructor[] constructorList=beanClass.getConstructors();

		for(Constructor constructor: constructorList){
			
			if(constructor.isAnnotationPresent(Autowired.class)){
				Class[] paramTypes=constructor.getParameterTypes();
				Object[] params=new Object[paramTypes.length];
				
				for(int i=0;i<paramTypes.length;i++){
					String str=(String)paramTypes[i].getName().replace('.','/');
					String[] str2=str.split("/");
					String beanName=str2[str2.length-1];
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

	@Override
	protected BeanDefinition GetCreatedBean(BeanDefinition beanDefinition) {
		// TODO Auto-generated method stub
		return null;
	}
}
