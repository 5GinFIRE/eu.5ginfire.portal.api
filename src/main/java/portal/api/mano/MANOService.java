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
