package cloudgene.mapred.server.services;

import java.util.ArrayList;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import cloudgene.mapred.apps.Application;
import cloudgene.mapred.apps.ApplicationRepository;
import cloudgene.mapred.core.Group;
import cloudgene.mapred.core.User;

@Singleton
public class GroupService {

	@Inject
	protected cloudgene.mapred.server.Application application;

	public List<Group> getAll() {
		List<Group> groups = new ArrayList<>();

		groups.add(new Group(User.ROLE_ADMIN));
		groups.add(new Group(User.ROLE_USER));
		groups.add(new Group(UserService.DEFAULT_ANONYMOUS_ROLE.toLowerCase()));

		ApplicationRepository repository = application.getSettings().getApplicationRepository();

		for (Application application : repository.getAll()) {
			for (String permission : application.getPermissions()) {
				Group group = new Group(permission);
				if (!groups.contains(group)) {
					group.addApp(application.getId());
					groups.add(group);
				} else {
					int index = groups.indexOf(group);
					group = groups.get(index);
					group.addApp(application.getId());
				}
			}
		}

		return groups;
	}
}
