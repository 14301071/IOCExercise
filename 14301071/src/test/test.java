package test;

import resource.LocalFileResource;
import factory.ApplicationContext;
import factory.ClassPathXmlApplicationContext;

public class test {

    public static void main(String[] args) {
    	LocalFileResource locations = new LocalFileResource("bean.xml");
        ApplicationContext ctx = 
		    new ClassPathXmlApplicationContext(locations);
        boss boss = (boss) ctx.getBean("boss");
    
        System.out.println(boss.tostring());
    }
}