package com.example.spring.ldap;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.ldap.repository.config.EnableLdapRepositories;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.DirContextSource;
import org.springframework.ldap.pool2.factory.PoolConfig;
import org.springframework.ldap.pool2.factory.PooledContextSource;
import org.springframework.ldap.pool2.validation.DefaultDirContextValidator;

import com.example.spring.ldap.repository.PersonLdapRepository;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableLdapRepositories
@PropertySource("classpath:application.properties")
public class SpringLdapPoolExample {

	// Set DEBUG level for org.springframework.ldap(.pool2)

	private static int c = 0;

	private InMemoryDirectoryServer server;

	@PostConstruct
	void postConstruct() throws LDAPException {
		final InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=example,dc=com");
		config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("embedded", 389));
		server = new InMemoryDirectoryServer(config);
		server.applyChangesFromLDIF("example.ldif");
		server.startListening();
		log.info("{} started.", server.getClass().getSimpleName());
	}

	@PreDestroy
	void preDestroy() {
		if (server != null) {
			server.shutDown(true);
			log.info("{} stopped", server.getClass().getSimpleName());
			server = null;
		}
	}

	@Bean
	ContextSource contextSource(@Value("${ldap.pooling.enabled:false}") boolean ldapPoolingEnabled) {
		final DirContextSource source = new DirContextSource() {
			@Override
			public DirContext getReadOnlyContext() {
				log.info("{} DirContextSource.getReadOnlyContext", ++c);
				final DirContext readOnlyContext = super.getReadOnlyContext();
				if (ldapPoolingEnabled) {
					final Thread t = new Thread(() -> {
						try {
							TimeUnit.MILLISECONDS.sleep(new Random().nextInt(4000) + 1000L);
							readOnlyContext.close(); // simulate failure after some time
							log.info("{} failed!", readOnlyContext);
						}
						catch (final NamingException e) {
							log.error(e.toString(), e);
						}
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
					});
					t.setPriority(Thread.MAX_PRIORITY);
					t.setDaemon(true);
					t.start();
				}
				return readOnlyContext;
			}

			@Override
			public DirContext getReadWriteContext() {
				throw new UnsupportedOperationException();
			}

			@Override
			public DirContext getContext(final String principal, final String credentials) {
				throw new UnsupportedOperationException();
			}
		};
		source.setUrl("ldap://localhost:" + server.getListenPort());
		return source;
	}

	@Bean
	@Primary
	@ConditionalOnProperty("ldap.pooling.enabled")
	PooledContextSource pooledContextSource(ContextSource wrapped) {
		final PoolConfig config = new PoolConfig();
		config.setMinEvictableIdleTimeMillis(2000);
		config.setTimeBetweenEvictionRunsMillis(2000);
		config.setTestOnBorrow(true);
		final PooledContextSource source = new PooledContextSource(config);
		source.setContextSource(wrapped);
		source.setDirContextValidator(new DefaultDirContextValidator());
		return source;
	}

	@Bean
	LdapTemplate ldapTemplate(ContextSource source) {
		log.info("{}", source);
		final LdapTemplate template = new LdapTemplate();
		template.setContextSource(source);
		return template;
	}

	public static void main(final String... args) throws InterruptedException {
		int i = 0;
		try (final ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(SpringLdapPoolExample.class)) {
			System.out.println("============================================================================");

			final PersonLdapRepository repo = context.getBean(PersonLdapRepository.class);
			for (int j = 0; j < 5; j++) {
				log.info("{}", repo.findBySnLikeIgnoreCase("Chil*"));
				TimeUnit.MILLISECONDS.sleep(100);
			}
			TimeUnit.MILLISECONDS.sleep(3000);

			final LdapOperations ldapOperations = context.getBean(LdapOperations.class);
			for (; i < 50; i++) {
				for (int j = 0; j < 5; j++) {
					log.info("{}", ldapOperations.list("dc=example,dc=com"));
					TimeUnit.MILLISECONDS.sleep(100);
				}
				TimeUnit.MILLISECONDS.sleep(3000);
			}

		}
		finally {
			log.info("{}/{}", c, i);
			System.out.println("============================================================================");
		}
	}

}
