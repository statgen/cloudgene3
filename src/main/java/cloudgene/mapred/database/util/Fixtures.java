package cloudgene.mapred.database.util;

import cloudgene.mapred.core.Template;
import cloudgene.mapred.core.User;
import cloudgene.mapred.database.TemplateDao;
import cloudgene.mapred.database.UserDao;
import cloudgene.mapred.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class Fixtures {

    private static final Logger log = LoggerFactory.getLogger(Fixtures.class);

    public static void insert(Database database) {
        insertUser(database, "admin", "admin1978", null, null, true, true);
        insertUser(database, "foobar", "foobarBAZ+42!", "Foo Bar", "foo@bar.com", false, true);
        insertUser(database, "snorlax", "Isnooze&12PM", "Snorlax", "snor@lax.jp", false, false);
    }

    private static void insertUser(Database database, String username, String password, String fullName, String mail, boolean isAdmin, boolean isActive) {

        // insert user
        UserDao dao = new UserDao(database);
        User user = dao.findByUsername(username);

        if (user == null) {
            user = new User();
            user.setUsername(username);
            String passwordHash = HashUtil.hashPassword(password);
            user.setPassword(passwordHash);
            user.setRoles(new String[]{User.ROLE_USER});
            user.setActive(isActive);

            if (fullName != null) user.setFullName(fullName);
            if (mail != null) user.setMail(mail);

            if (isAdmin) {
                user.setRoles(new String[]{User.ROLE_ADMIN, User.ROLE_USER});
            } else {
                user.setRoles(new String[]{User.ROLE_USER});
            }

            dao.insert(user);
            log.info("User {} created.", username);
        } else {
            log.info("User {} already exists.", username);

            if (isAdmin && !user.isAdmin()) {
                String[] oldRoles = user.getRoles();
                String[] newRoles = Arrays.copyOf(oldRoles, oldRoles.length + 1);
                newRoles[newRoles.length - 1] = User.ROLE_ADMIN;
                user.setRoles(newRoles);

                dao.update(user);
                log.info("User {} has admin rights now.", username);
            }
        }

        // insert template messages
        TemplateDao htmlSnippetDao = new TemplateDao(database);

        for (Template defaultSnippet : Template.SNIPPETS) {
            Template snippet = htmlSnippetDao.findByKey(defaultSnippet.getKey());
            if (snippet == null) {
                htmlSnippetDao.insert(defaultSnippet);
                log.info("Template {} created.", defaultSnippet.getKey());
            } else {
                log.info("Template {} already exists.", defaultSnippet.getKey());
            }
        }
    }
}
