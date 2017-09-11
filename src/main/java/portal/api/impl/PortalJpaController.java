/**
 * Copyright 2017 University of Patras 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at:
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * See the License for the specific language governing permissions and limitations under the License.
 */

package portal.api.impl;

import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import portal.api.model.ExperimentMetadata;
import portal.api.model.PortalProperty;
import portal.api.model.PortalUser;
import portal.api.model.VxFMetadata;
import portal.api.model.Category;
import portal.api.model.DeploymentDescriptor;
import portal.api.model.InstalledVxF;
import portal.api.model.MANOplatform;
import portal.api.model.Product;
import portal.api.model.SubscribedResource;

/**
 * This class maintains the entity manager and get a broker element from DB
 * 
 * @author ctranoris
 * 
 */
public class PortalJpaController {
	private static final transient Log logger = LogFactory.getLog(PortalJpaController.class.getName());

	@PersistenceUnit
	private EntityManagerFactory entityManagerFactory;

	public void initData() {
		PortalUser admin = readPortalUserById(1);
		logger.info("======================== admin  = " + admin);

		if (admin == null) {
			PortalUser bu = new PortalUser();
			bu.setName("Portal Administrator");
			bu.setUsername("admin");
			bu.setPassword("changeme");
			bu.setEmail("");
			bu.setOrganization("");
			bu.setRole("ROLE_PORTALADMIN");
			bu.setActive(true);
			saveUser(bu);

			Category c = new Category();
			c.setName("None");
			saveCategory(c);

			PortalProperty p = new PortalProperty();
			p.setName("adminEmail");
			p.setValue("info@example.org");
			saveProperty(p);
			p = new PortalProperty();
			p.setName("activationEmailSubject");
			p.setValue("Activation Email Subject");
			saveProperty(p);
			p = new PortalProperty("mailhost", "example.org");
			saveProperty(p);
			p = new PortalProperty("mailuser", "exampleusername");
			saveProperty(p);
			p = new PortalProperty("mailpassword", "pass");
			saveProperty(p);

		}

	}

	public long countInstalledVxFs() {

		EntityManager entityManager = entityManagerFactory.createEntityManager();

		Query q = entityManager.createQuery("SELECT COUNT(s) FROM InstalledVxF s");
		return (Long) q.getSingleResult();
	}

	public InstalledVxF updateInstalledVxF(InstalledVxF is) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();

		EntityTransaction entityTransaction = entityManager.getTransaction();

		entityTransaction.begin();
		InstalledVxF resis = entityManager.merge(is);
		entityTransaction.commit();

		return resis;
	}

	public void saveInstalledVxF(InstalledVxF is) {
		logger.info("Will create InstalledVxF = " + is.getUuid());

		EntityManager entityManager = entityManagerFactory.createEntityManager();

		EntityTransaction entityTransaction = entityManager.getTransaction();

		entityTransaction.begin();

		entityManager.persist(is);
		entityManager.flush();
		entityTransaction.commit();
	}

	public InstalledVxF readInstalledVxFByName(final String name) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();

		Query q = entityManager.createQuery("SELECT m FROM InstalledVxF m WHERE m.name='" + name + "'");
		return (q.getResultList().size() == 0) ? null : (InstalledVxF) q.getSingleResult();
	}

	public InstalledVxF readInstalledVxFByUUID(final String uuid) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();

		Query q = entityManager.createQuery("SELECT m FROM InstalledVxF m WHERE m.uuid='" + uuid + "'");
		return (q.getResultList().size() == 0) ? null : (InstalledVxF) q.getSingleResult();
	}

	@SuppressWarnings("unchecked")
	public List<InstalledVxF> readInstalledVxFs(int firstResult, int maxResults) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();

		Query q = entityManager.createQuery("SELECT m FROM InstalledVxF m");
		q.setFirstResult(firstResult);
		q.setMaxResults(maxResults);
		return q.getResultList();
	}

	public void deleteInstalledVxF(final InstalledVxF message) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();

		EntityTransaction entityTransaction = entityManager.getTransaction();

		entityTransaction.begin();

		entityManager.remove(message);

		entityTransaction.commit();
	}

	public void getAllInstalledVxFPrinted() {
		logger.info("================= getAll() ==================START");

		EntityManager entityManager = entityManagerFactory.createEntityManager();

		List<InstalledVxF> lb = entityManager.createQuery("select p from InstalledVxF p").getResultList();
		for (Iterator iterator = lb.iterator(); iterator.hasNext();) {
			InstalledVxF iVxF = (InstalledVxF) iterator.next();
			logger.info("=== InstalledVxF found: " + iVxF.getName() + ", Id: " + iVxF.getId() + ", Uuid: "
					+ iVxF.getUuid() + ", RepoUrl: " + iVxF.getRepoUrl() + ", InstalledVersion: "
					+ iVxF.getInstalledVersion() + ", PackageURL: " + iVxF.getPackageURL() + ", PackageLocalPath: "
					+ iVxF.getPackageLocalPath() + ", Status: " + iVxF.getStatus());

		}

		logger.info("================= getAll() ==================END");
	}

	public PortalJpaController() {
		logger.info(">>>>>>>>>>>>>> PortalJpaController constructor  <<<<<<<<<<<<<<<<<<");

	}

	public String echo(String message) {
		return "Echo processed: " + message;

	}

	public void deleteAllInstalledVxFs() {
		EntityManager entityManager = entityManagerFactory.createEntityManager();

		EntityTransaction entityTransaction = entityManager.getTransaction();

		entityTransaction.begin();

		Query q = entityManager.createQuery("DELETE FROM InstalledVxF ");
		q.executeUpdate();
		entityManager.flush();

		entityTransaction.commit();
	}

	public void deleteAllProducts() {
		EntityManager entityManager = entityManagerFactory.createEntityManager();

		EntityTransaction entityTransaction = entityManager.getTransaction();

		entityTransaction.begin();

		Query q = entityManager.createQuery("DELETE FROM Product ");
		q.executeUpdate();
		entityManager.flush();

		entityTransaction.commit();

	}

	public void deleteAllUsers() {
		EntityManager entityManager = entityManagerFactory.createEntityManager();

		EntityTransaction entityTransaction = entityManager.getTransaction();

		entityTransaction.begin();

		Query q = entityManager.createQuery("DELETE FROM PortalUser ");
		q.executeUpdate();
		entityManager.flush();

		entityTransaction.commit();

	}

	public void saveUser(PortalUser bu) {
		logger.info("Will save PortalUser = " + bu.getName());

		EntityManager entityManager = entityManagerFactory.createEntityManager();

		EntityTransaction entityTransaction = entityManager.getTransaction();

		entityTransaction.begin();

		entityManager.persist(bu);

		entityManager.flush();
		entityTransaction.commit();

	}

	public PortalUser readPortalUserByUsername(String username) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		Query q = entityManager.createQuery("SELECT m FROM PortalUser m WHERE m.username='" + username + "'");
		return (q.getResultList().size() == 0) ? null : (PortalUser) q.getSingleResult();
	}

	public PortalUser readPortalUserBySessionID(String id) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		Query q = entityManager.createQuery("SELECT m FROM PortalUser m WHERE m.currentSessionID='" + id + "'");
		return (q.getResultList().size() == 0) ? null : (PortalUser) q.getSingleResult();
	}

	public PortalUser readPortalUserByEmail(String email) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		Query q = entityManager.createQuery("SELECT m FROM PortalUser m WHERE m.email='" + email + "'");
		return (q.getResultList().size() == 0) ? null : (PortalUser) q.getSingleResult();
	}

	public PortalUser readPortalUserById(int userid) {

		EntityManager entityManager = entityManagerFactory.createEntityManager();
		return entityManager.find(PortalUser.class, userid);

		// Query q = entityManager.createQuery("SELECT m FROM PortalUser m WHERE
		// m.id=" + userid );
		// return (q.getResultList().size()==0)?null:(PortalUser)
		// q.getSingleResult();

	}

	public PortalUser updatePortalUser(PortalUser bu) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();

		EntityTransaction entityTransaction = entityManager.getTransaction();

		entityTransaction.begin();
		PortalUser resis = entityManager.merge(bu);
		entityTransaction.commit();

		return resis;
	}

	@SuppressWarnings("unchecked")
	public List<PortalUser> readUsers(int firstResult, int maxResults) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();

		Query q = entityManager.createQuery("SELECT m FROM PortalUser m");
		q.setFirstResult(firstResult);
		q.setMaxResults(maxResults);
		return q.getResultList();
	}

	public long countUsers() {

		EntityManager entityManager = entityManagerFactory.createEntityManager();

		Query q = entityManager.createQuery("SELECT COUNT(s) FROM PortalUser s");
		return (Long) q.getSingleResult();
	}

	public void getAllUsersPrinted() {
		logger.info("================= getAllUsers() ==================START");

		EntityManager entityManager = entityManagerFactory.createEntityManager();

		List<PortalUser> lb = entityManager.createQuery("select p from PortalUser p").getResultList();
		for (Iterator iterator = lb.iterator(); iterator.hasNext();) {
			PortalUser bu = (PortalUser) iterator.next();
			logger.info("	======> PortalUser found: " + bu.getName() + ", Id: " + bu.getId() + ", Id: "
					+ bu.getOrganization() + ", username: " + bu.getUsername());

			List<Product> products = bu.getProducts();
			for (Product prod : products) {
				logger.info("	======> vxfMetadata found: " + prod.getName() + ", Id: " + prod.getId() + ", getUuid: "
						+ prod.getUuid() + ", getName: " + prod.getName());
			}

		}
		logger.info("================= getAll() ==================END");
	}

	public void saveProduct(Product prod) {
		logger.info("Will save Product = " + prod.getName());

		EntityManager entityManager = entityManagerFactory.createEntityManager();

		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.persist(prod);
		entityManager.flush();
		entityTransaction.commit();

	}

	public Product updateProduct(Product bm) {
		logger.info("================= updateProduct ==================");
		logger.info("bmgetId=" + bm.getId());
		logger.info("bm getName= " + bm.getName());
		EntityManager entityManager = entityManagerFactory.createEntityManager();

		EntityTransaction entityTransaction = entityManager.getTransaction();

		entityTransaction.begin();
		Product resis = entityManager.merge(bm);
		entityTransaction.commit();

		return resis;
	}

	// public VxFMetadata updateVxFMetadata(VxFMetadata bm) {
	// logger.info("================= updateVxFMetadata ==================");
	// logger.info("bmgetId="+bm.getId());
	// logger.info("bm getName= "+bm.getName());
	// logger.info("bm getPackageLocation= "+bm.getPackageLocation());
	// EntityManager entityManager = entityManagerFactory.createEntityManager();
	//
	// EntityTransaction entityTransaction = entityManager.getTransaction();
	//
	// entityTransaction.begin();
	// VxFMetadata resis = entityManager.merge(bm);
	// entityTransaction.commit();
	//
	// return resis;
	// }

	@SuppressWarnings("unchecked")
	public List<VxFMetadata> readVxFsMetadata(Long categoryid, int firstResult, int maxResults) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		// Query q = entityManager.createQuery("SELECT m FROM VxFMetadata m");
		Query q;

		if ((categoryid != null) && (categoryid >= 0))
			q = entityManager
					.createQuery("SELECT a FROM VxFMetadata a WHERE a.categories.id=" + categoryid + " ORDER BY a.id");
		else
			q = entityManager.createQuery("SELECT a FROM VxFMetadata a ORDER BY a.id");

		q.setFirstResult(firstResult);
		q.setMaxResults(maxResults);
		return q.getResultList();
	}

	@SuppressWarnings("unchecked")
	public List<VxFMetadata> readVxFsMetadataForOwnerID(Long ownerid, int firstResult, int maxResults) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		// Query q = entityManager.createQuery("SELECT m FROM VxFMetadata m");
		Query q;

		if ((ownerid != null) && (ownerid >= 0))
			q = entityManager.createQuery("SELECT a FROM VxFMetadata a WHERE a.owner.id=" + ownerid + " ORDER BY a.id");
		else
			q = entityManager.createQuery("SELECT a FROM VxFMetadata a ORDER BY a.id");

		q.setFirstResult(firstResult);
		q.setMaxResults(maxResults);
		return q.getResultList();
	}

	public Product readProductByUUID(String uuid) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();

		Query q = entityManager.createQuery("SELECT m FROM Product m WHERE m.uuid='" + uuid + "'");
		return (q.getResultList().size() == 0) ? null : (Product) q.getSingleResult();
	}

	public Product readProductByID(long id) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		Product u = entityManager.find(Product.class, id);
		return u;
	}

	// public VxFMetadata readVxFMetadataByUUID(String uuid) {
	// EntityManager entityManager = entityManagerFactory.createEntityManager();
	//
	// Query q = entityManager.createQuery("SELECT m FROM VxFMetadata m WHERE
	// m.uuid='" + uuid + "'");
	// return (q.getResultList().size()==0)?null:(VxFMetadata)
	// q.getSingleResult();
	// }
	//
	// public VxFMetadata readVxFMetadataByID(int vxfid) {
	// EntityManager entityManager = entityManagerFactory.createEntityManager();
	// VxFMetadata u = entityManager.find(VxFMetadata.class, vxfid);
	// return u;
	// }

	public void deleteUser(int userid) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		PortalUser u = entityManager.find(PortalUser.class, userid);

		EntityTransaction entityTransaction = entityManager.getTransaction();

		entityTransaction.begin();

		entityManager.remove(u);

		entityTransaction.commit();
	}

	public void deleteProduct(int id) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		Product p = entityManager.find(Product.class, id);

		EntityTransaction entityTransaction = entityManager.getTransaction();

		entityTransaction.begin();
		entityManager.remove(p);
		entityTransaction.commit();
	}

	@SuppressWarnings("unchecked")
	public List<Product> readProducts(Long categoryid, int firstResult, int maxResults) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		Query q;

		if ((categoryid != null) && (categoryid >= 0))
			q = entityManager
					.createQuery("SELECT a FROM Product a WHERE a.category.id=" + categoryid + " ORDER BY a.id");
		else
			q = entityManager.createQuery("SELECT a FROM Product a ORDER BY a.id");

		q.setFirstResult(firstResult);
		q.setMaxResults(maxResults);
		return q.getResultList();
	}

	public void getAllProductsPrinted() {
		logger.info("================= getAllProductsPrinted() ==================START");

		EntityManager entityManager = entityManagerFactory.createEntityManager();

		List<Product> lb = readProducts(null, 0, 10000);
		for (Iterator iterator = lb.iterator(); iterator.hasNext();) {
			Product prod = (Product) iterator.next();

			logger.info("	=================> Product found: " + prod.getName() + ", Id: " + prod.getId()
					+ ", getUuid: " + prod.getUuid() + ", getName: " + prod.getName() + ", Owner.name: "
					+ prod.getOwner().getName());

		}
		logger.info("================= getAllProductsPrinted() ==================END");

	}

	public void saveSubscribedResource(SubscribedResource sm) {
		logger.info("Will save SubscribedResource = " + sm.getURL());

		EntityManager entityManager = entityManagerFactory.createEntityManager();

		EntityTransaction entityTransaction = entityManager.getTransaction();

		entityTransaction.begin();

		entityManager.persist(sm);
		entityManager.flush();
		entityTransaction.commit();

	}

	public SubscribedResource updateSubscribedResource(SubscribedResource sm) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();

		EntityTransaction entityTransaction = entityManager.getTransaction();

		entityTransaction.begin();
		SubscribedResource resis = entityManager.merge(sm);
		entityTransaction.commit();

		return resis;
	}

	public SubscribedResource readSubscribedResourceById(int userid) {

		EntityManager entityManager = entityManagerFactory.createEntityManager();
		return entityManager.find(SubscribedResource.class, userid);

	}

	public SubscribedResource readSubscribedResourceByuuid(String uuid) {

		EntityManager entityManager = entityManagerFactory.createEntityManager();

		Query q = entityManager.createQuery("SELECT m FROM SubscribedResource m WHERE m.uuid='" + uuid + "'");
		return (q.getResultList().size() == 0) ? null : (SubscribedResource) q.getSingleResult();

	}

	public void deleteSubscribedResource(int smId) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		SubscribedResource sm = entityManager.find(SubscribedResource.class, smId);

		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.remove(sm);
		entityTransaction.commit();
	}

	public long countSubscribedResources() {

		EntityManager entityManager = entityManagerFactory.createEntityManager();

		Query q = entityManager.createQuery("SELECT COUNT(s) FROM SubscribedResource s");
		return (Long) q.getSingleResult();
	}

	public void getAllSubscribedResourcesPrinted() {
		logger.info("================= getSubscribedResource() ==================START");

		EntityManager entityManager = entityManagerFactory.createEntityManager();

		List<SubscribedResource> lb = entityManager.createQuery("select p from SubscribedResource p").getResultList();
		for (Iterator iterator = lb.iterator(); iterator.hasNext();) {
			SubscribedResource sm = (SubscribedResource) iterator.next();
			logger.info("	======> SubscribedResource found: " + sm.getURL() + ", Id: " + sm.getId());

		}
	}

	public void deleteAllSubscribedResources() {
		EntityManager entityManager = entityManagerFactory.createEntityManager();

		EntityTransaction entityTransaction = entityManager.getTransaction();

		entityTransaction.begin();

		Query q = entityManager.createQuery("DELETE FROM SubscribedResource ");
		q.executeUpdate();
		entityManager.flush();

		entityTransaction.commit();

	}

	public List<SubscribedResource> readSubscribedResources(int firstResult, int maxResults) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();

		Query q = entityManager.createQuery("SELECT m FROM SubscribedResource m");
		q.setFirstResult(firstResult);
		q.setMaxResults(maxResults);
		return q.getResultList();
	}

	public List<ExperimentMetadata> readAppsMetadata(Long categoryid, int firstResult, int maxResults) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		Query q;

		if ((categoryid != null) && (categoryid >= 0))
			q = entityManager.createQuery(
					"SELECT a FROM ExperimentMetadata a WHERE a.categories.id=" + categoryid + " ORDER BY a.id");
		else
			q = entityManager.createQuery("SELECT a FROM ExperimentMetadata a ORDER BY a.id");
		q.setFirstResult(firstResult);
		q.setMaxResults(maxResults);
		return q.getResultList();
	}

	@SuppressWarnings("unchecked")
	public List<ExperimentMetadata> readAppsMetadataForOwnerID(Long ownerid, int firstResult, int maxResults) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		// Query q = entityManager.createQuery("SELECT m FROM VxFMetadata m");
		Query q;

		if ((ownerid != null) && (ownerid >= 0))
			q = entityManager
					.createQuery("SELECT a FROM ExperimentMetadata a WHERE a.owner.id=" + ownerid + " ORDER BY a.id");
		else
			q = entityManager.createQuery("SELECT a FROM ExperimentMetadata a ORDER BY a.id");

		q.setFirstResult(firstResult);
		q.setMaxResults(maxResults);
		return q.getResultList();
	}

	public List<Category> readCategories(int firstResult, int maxResults) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();

		Query q = entityManager.createQuery("SELECT m FROM Category m  ORDER BY m.id");
		q.setFirstResult(firstResult);
		q.setMaxResults(maxResults);
		return q.getResultList();
	}

	public void saveCategory(Category c) {
		logger.info("Will category = " + c.getName());

		EntityManager entityManager = entityManagerFactory.createEntityManager();

		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();

		entityManager.persist(c);
		entityManager.flush();
		entityTransaction.commit();
	}

	public Category readCategoryByID(int catid) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		Category u = entityManager.find(Category.class, catid);
		return u;
	}

	public Category updateCategory(Category c) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();

		EntityTransaction entityTransaction = entityManager.getTransaction();

		entityTransaction.begin();
		Category resis = entityManager.merge(c);
		entityTransaction.commit();

		return resis;
	}

	public void deleteCategory(int catid) {

		EntityManager entityManager = entityManagerFactory.createEntityManager();
		Category c = entityManager.find(Category.class, catid);

		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.remove(c);
		entityTransaction.commit();

	}

	public void deleteAllCategories() {
		EntityManager entityManager = entityManagerFactory.createEntityManager();

		EntityTransaction entityTransaction = entityManager.getTransaction();

		entityTransaction.begin();

		Query q = entityManager.createQuery("DELETE FROM Category");
		q.executeUpdate();
		entityManager.flush();

		entityTransaction.commit();

	}

	public void getAllCategoriesPrinted() {
		logger.info("================= getAllCategoriesPrinted() ==================START");

		EntityManager entityManager = entityManagerFactory.createEntityManager();

		List<Category> lb = entityManager.createQuery("select p from Category p").getResultList();
		for (Iterator iterator = lb.iterator(); iterator.hasNext();) {
			Category sm = (Category) iterator.next();
			logger.info("	======> Category found: " + sm.getName() + ", Id: " + sm.getId());

		}

	}

	public void saveProperty(PortalProperty p) {
		logger.info("Will PortalProperty = " + p.getName());

		EntityManager entityManager = entityManagerFactory.createEntityManager();

		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();

		entityManager.persist(p);
		entityManager.flush();
		entityTransaction.commit();

	}

	public void deleteProperty(int propid) {

		EntityManager entityManager = entityManagerFactory.createEntityManager();
		PortalProperty c = entityManager.find(PortalProperty.class, propid);

		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.remove(c);
		entityTransaction.commit();

	}

	public PortalProperty updateProperty(PortalProperty p) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();

		EntityTransaction entityTransaction = entityManager.getTransaction();

		entityTransaction.begin();
		PortalProperty bp = entityManager.merge(p);
		entityTransaction.commit();

		return bp;
	}

	public List<PortalProperty> readProperties(int firstResult, int maxResults) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();

		Query q = entityManager.createQuery("SELECT m FROM PortalProperty m  ORDER BY m.id");
		q.setFirstResult(firstResult);
		q.setMaxResults(maxResults);
		return q.getResultList();

	}

	public PortalProperty readPropertyByName(String name) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();

		Query q = entityManager.createQuery("SELECT m FROM PortalProperty m WHERE m.name='" + name + "'");
		return (q.getResultList().size() == 0) ? null : (PortalProperty) q.getSingleResult();

	}

	public PortalProperty readPropertyByID(int propid) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		PortalProperty u = entityManager.find(PortalProperty.class, propid);
		return u;

	}

	public List<DeploymentDescriptor> readDeploymentDescriptors(int firstResult, int maxResults) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();

		Query q = entityManager.createQuery("SELECT m FROM DeploymentDescriptor m  ORDER BY m.id");
		q.setFirstResult(firstResult);
		q.setMaxResults(maxResults);
		return q.getResultList();
	}

	public void deleteDeployment(int id) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		DeploymentDescriptor c = entityManager.find(DeploymentDescriptor.class, id);

		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.remove(c);
		entityTransaction.commit();

	}

	public DeploymentDescriptor readDeploymentByID(int deploymentId) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		DeploymentDescriptor u = entityManager.find(DeploymentDescriptor.class, deploymentId);
		return u;
	}

	public DeploymentDescriptor updateDeploymentDescriptor(DeploymentDescriptor d) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();

		EntityTransaction entityTransaction = entityManager.getTransaction();

		entityTransaction.begin();
		DeploymentDescriptor resis = entityManager.merge(d);
		entityTransaction.commit();

		return resis;
	}

	public void saveMANOplatform(MANOplatform mp) {
		logger.info("Will save MANOplatform = " + mp.getName());

		EntityManager entityManager = entityManagerFactory.createEntityManager();

		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();
		entityManager.persist(mp);
		entityManager.flush();
		entityTransaction.commit();

	}

	public long countMANOplatforms() {

		EntityManager entityManager = entityManagerFactory.createEntityManager();

		Query q = entityManager.createQuery("SELECT COUNT(s) FROM MANOplatform s");
		return (Long) q.getSingleResult();
	}

	public MANOplatform readMANOplatformById(int i) {

		EntityManager entityManager = entityManagerFactory.createEntityManager();
		return entityManager.find(MANOplatform.class, i);
	}

	public void deleteAllMANOplatforms() {
		EntityManager entityManager = entityManagerFactory.createEntityManager();

		EntityTransaction entityTransaction = entityManager.getTransaction();

		entityTransaction.begin();

		Query q = entityManager.createQuery("DELETE FROM MANOplatform");
		q.executeUpdate();
		entityManager.flush();

		entityTransaction.commit();

	}

	public MANOplatform readMANOplatformByName(String name) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();

		Query q = entityManager.createQuery("SELECT m FROM MANOplatform m WHERE m.name='" + name + "'");
		return (q.getResultList().size() == 0) ? null : (MANOplatform) q.getSingleResult();
	}

}
