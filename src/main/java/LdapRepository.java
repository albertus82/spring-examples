import org.springframework.data.ldap.repository.support.SimpleLdapRepository;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.odm.core.ObjectDirectoryMapper;

public class LdapRepository extends SimpleLdapRepository<String> {

	public LdapRepository(LdapOperations ldapOperations, ObjectDirectoryMapper odm, Class<String> entityType) {
		super(ldapOperations, odm, entityType);
	}

}
