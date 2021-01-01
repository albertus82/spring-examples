import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.naming.directory.DirContext;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.DirContextSource;
import org.springframework.ldap.pool2.factory.PoolConfig;
import org.springframework.ldap.pool2.factory.PooledContextSource;
import org.springframework.ldap.pool2.validation.DefaultDirContextValidator;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class App {

	private static int c = 0;

	private InMemoryDirectoryServer server;

	@PostConstruct
	void postConstruct() throws LDAPException {
		final InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=example,dc=com");
		config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("embedded", 389));
		server = new InMemoryDirectoryServer(config);
		server.applyChangesFromLDIF("example.ldif");
		server.startListening();
		log.info("server started");
	}

	@PreDestroy
	void preDestroy() {
		if (server != null) {
			server.shutDown(true);
			log.info("server stopped");
		}
	}

	@Bean
	ContextSource contextSource() {
		final DirContextSource source = new DirContextSource() {
			@Override
			public DirContext getReadOnlyContext() {
				log.info("{} DirContextSource.getReadOnlyContext", ++c);
				return super.getReadOnlyContext();
			}

			@Override
			public DirContext getReadWriteContext() {
				throw new UnsupportedOperationException();
			}

			@Override
			public DirContext getContext(String principal, String credentials) {
				throw new UnsupportedOperationException();
			}
		};
		source.setUrl("ldap://localhost:" + server.getListenPort());
		return source;
	}

	@Bean
	@Primary
	ContextSource pooledContextSource(ContextSource wrapped) {
		final PoolConfig config = new PoolConfig();
		config.setMinEvictableIdleTimeMillis(20000);
		config.setTimeBetweenEvictionRunsMillis(20000);
		config.setTestOnBorrow(true);
		final PooledContextSource source = new PooledContextSource(config);
		source.setContextSource(wrapped);
		source.setDirContextValidator(new DefaultDirContextValidator());
		return source;
	}

	@Bean
	LdapOperations ldapOperations(ContextSource source) {
		final LdapTemplate template = new LdapTemplate();
		template.setContextSource(source);
		return template;
	}

	public static void main(final String... args) throws InterruptedException {
		int j = 0;
		try (final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(App.class)) {
			LdapOperations l = context.getBean(LdapOperations.class);
			for (; j < 50; j++) {
				for (int i = 0; i < 5; i++) {
					log.info("{} {}", new Date(), l.list("dc=example,dc=com"));
				}
				TimeUnit.MILLISECONDS.sleep(30000);
			}
		}
		finally {
			log.info("{}/{}", c, j);
		}
	}

}
