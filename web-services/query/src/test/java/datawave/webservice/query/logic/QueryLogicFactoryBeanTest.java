package datawave.webservice.query.logic;

import datawave.security.authorization.DatawavePrincipal;
import org.easymock.EasyMockExtension;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.ejb.EJBContext;

@Disabled
@ExtendWith(EasyMockExtension.class)
public class QueryLogicFactoryBeanTest extends EasyMockSupport {
    
    QueryLogicFactoryImpl bean = new QueryLogicFactoryImpl();
    
    @Mock
    QueryLogicFactoryConfiguration altFactoryConfig;
    
    @Mock
    DatawavePrincipal altPrincipal;
    
    @Mock
    ClassPathXmlApplicationContext applicationContext;
    
    BaseQueryLogic<?> logic;
    
    private QueryLogicFactoryConfiguration factoryConfig = null;
    private EJBContext ctx;
    private DatawavePrincipal principal = null;
    //
    // @BeforeEach
    // public void setup() throws IllegalArgumentException, IllegalAccessException {
    // System.setProperty(NpeUtils.NPE_OU_PROPERTY, "iamnotaperson");
    // System.setProperty("dw.metadatahelper.all.auths", "A,B,C,D");
    // Logger.getLogger(ClassPathXmlApplicationContext.class).setLevel(Level.OFF);
    // Logger.getLogger(XmlBeanDefinitionReader.class).setLevel(Level.OFF);
    // Logger.getLogger(DefaultListableBeanFactory.class).setLevel(Level.OFF);
    // ClassPathXmlApplicationContext queryFactory = new ClassPathXmlApplicationContext();
    // queryFactory.setConfigLocation("TestQueryLogicFactory.xml");
    // queryFactory.refresh();
    // factoryConfig = queryFactory.getBean(QueryLogicFactoryConfiguration.class.getSimpleName(), QueryLogicFactoryConfiguration.class);
    //
    // ReflectionTestUtils.setField(bean, "queryLogicFactoryConfiguration", factoryConfig);
    // ReflectionTestUtils.setField(bean, "applicationContext", queryFactory);
    //
    // ctx = createMock(EJBContext.class);
    // logic = createMockBuilder(BaseQueryLogic.class).addMockedMethods("setLogicName", "getMaxPageSize", "getPageByteTrigger").createMock();
    // DatawaveUser user = new DatawaveUser(SubjectIssuerDNPair.of("CN=Poe Edgar Allan eapoe, OU=acme", "<CN=ca, OU=acme>"), UserType.USER, null, null, null,
    // 0L);
    // principal = new DatawavePrincipal(Collections.singletonList(user));
    // }
    //
    // @Test
    // public void testGetQueryLogicWrongName() throws IllegalArgumentException, CloneNotSupportedException {
    // EasyMock.expect(ctx.getCallerPrincipal()).andReturn(principal);
    // assertThrows(IllegalArgumentException.class, () ->bean.getQueryLogic("MyQuery", principal));
    // }
    //
    // @Test
    // public void testGetQueryLogic() throws IllegalArgumentException, CloneNotSupportedException {
    // EasyMock.expect(ctx.getCallerPrincipal()).andReturn(principal);
    // TestQueryLogic<?> logic = (TestQueryLogic<?>) bean.getQueryLogic("TestQuery", principal);
    // assertEquals("MyMetadataTable", logic.getTableName());
    // assertEquals(12345, logic.getMaxResults());
    // assertEquals(98765, logic.getMaxWork());
    // }
    //
    // @Test
    // public void testGetQueryLogic_HasRequiredRoles() throws Exception {
    // // Set the query name
    // String queryName = "TestQuery";
    // Collection<String> roles = Arrays.asList("Monkey King", "Monkey Queen");
    //
    // // Set expectations
    // QueryLogicFactoryConfiguration qlfc = new QueryLogicFactoryConfiguration();
    // qlfc.setMaxPageSize(25);
    // qlfc.setPageByteTrigger(1024L);
    // this.logic.setPrincipal(altPrincipal);
    // this.logic.setLogicName(queryName);
    // expect(this.logic.getMaxPageSize()).andReturn(25);
    // expect(this.logic.getPageByteTrigger()).andReturn(1024L);
    // expect(this.applicationContext.getBean(queryName)).andReturn(this.logic);
    //
    // // Run the test
    // replayAll();
    // QueryLogicFactoryImpl subject = new QueryLogicFactoryImpl();
    // // Field f = loginModule.getClass().getDeclaredField("identity");
    // // f.setAccessible(true);
    // // DatawavePrincipal principal = (DatawavePrincipal) f.get(loginModule);
    //
    // ReflectionTestUtils.getField(QueryLogicFactoryImpl.class, "queryLogicFactoryConfiguration").set(subject, factoryConfig);
    // Whitebox.getField(QueryLogicFactoryImpl.class, "applicationContext").set(subject, this.applicationContext);
    // QueryLogic<?> result1 = subject.getQueryLogic(queryName, this.altPrincipal);
    // verifyAll();
    //
    // // Verify results
    // assertSame(this.logic, result1, "Query logic should not return null");
    // }
    //
    // @Test
    // public void testGetQueryLogic_propertyOverride() throws Exception {
    // // Set the query name
    // String queryName = "TestQuery";
    // Collection<String> roles = Arrays.asList("Monkey King", "Monkey Queen");
    //
    // // Set expectations
    // QueryLogicFactoryConfiguration qlfc = new QueryLogicFactoryConfiguration();
    // qlfc.setMaxPageSize(25);
    // qlfc.setPageByteTrigger(1024L);
    //
    // Map<String,Collection<String>> rolesMap = new HashMap<>();
    // rolesMap.put(queryName, roles);
    //
    // this.logic.setPrincipal(altPrincipal);
    // this.logic.setLogicName(queryName);
    // expect(this.logic.getMaxPageSize()).andReturn(0);
    // expect(this.logic.getPageByteTrigger()).andReturn(0L);
    // this.logic.setMaxPageSize(25);
    // this.logic.setPageByteTrigger(1024L);
    // expect(this.applicationContext.getBean(queryName)).andReturn(this.logic);
    //
    // // Run the test
    // replayAll();
    // QueryLogicFactoryImpl subject = new QueryLogicFactoryImpl();
    // Whitebox.getField(QueryLogicFactoryImpl.class, "queryLogicFactoryConfiguration").set(subject, qlfc);
    // Whitebox.getField(QueryLogicFactoryImpl.class, "applicationContext").set(subject, this.applicationContext);
    // QueryLogic<?> result1 = subject.getQueryLogic(queryName, this.altPrincipal);
    // verifyAll();
    //
    // // Verify results
    // assertSame(this.logic, result1, "Query logic should not return null");
    // }
    //
    // @Test
    // public void testQueryLogicList() throws Exception {
    // // Set expectations
    // Map<String,QueryLogic> logicClasses = new TreeMap<>();
    // logicClasses.put("TestQuery", this.logic);
    // expect(this.applicationContext.getBeansOfType(QueryLogic.class)).andReturn(logicClasses);
    // this.logic.setLogicName("TestQuery");
    //
    // // Run the test
    // replayAll();
    // QueryLogicFactoryImpl subject = new QueryLogicFactoryImpl();
    // Whitebox.getField(QueryLogicFactoryImpl.class, "queryLogicFactoryConfiguration").set(subject, this.altFactoryConfig);
    // Whitebox.getField(QueryLogicFactoryImpl.class, "applicationContext").set(subject, this.applicationContext);
    // List<QueryLogic<?>> result1 = subject.getQueryLogicList();
    // verifyAll();
    //
    // // Verify results
    // assertNotNull(result1, "Query logic list should not return null");
    // assertEquals(1, result1.size(), "Query logic list should return with 1 item");
    // }
}
