package portal.api.mano;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import portal.api.model.MANOprovider;

public class MANOService {
	protected EntityManager em;
	
	public MANOService(EntityManager em)
	{
		this.em=em;
	}
	
	public List<MANOprovider> getAllMANOproviders()
	{
		TypedQuery<MANOprovider> query = em.createQuery("SELECT mp FROM MANOprovider mp",MANOprovider.class);
		return query.getResultList();
	}
	
	public List<MANOprovider> getMANOprovidersEnabledForOnboarding()
	{
		TypedQuery<MANOprovider> query = em.createQuery("SELECT mp FROM MANOprovider mp WHERE mp.enabledForONBOARDING=1",MANOprovider.class);
		return query.getResultList();
	}
	
}
