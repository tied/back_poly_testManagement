package com.thed.zephyr.je.zql.model;

import net.java.ao.Entity;
import net.java.ao.OneToMany;
import net.java.ao.Preload;
import net.java.ao.schema.Default;
import net.java.ao.schema.StringLength;

@Preload
public interface ZQLFilter extends Entity{
	
	public String getCreatedBy();
	public void setCreatedBy(String createdBy);
	
	public String getFilterName();
	public void setFilterName(String filterName);
	
	public String getUpdatedBy();
	public void setUpdatedBy(String updatedBy);
	
	public Long getCreatedOn();
	public void setCreatedOn(Long createdOn);
	
	public Long getUpdatedOn();
	public void setUpdatedOn(Long updatedOn);
	
	@StringLength(StringLength.UNLIMITED)
	public String getZqlQuery();
	@StringLength(StringLength.UNLIMITED)
	public void setZqlQuery(String zqlQuery);
	
    public String getDescription();    
    public void setDescription(String description);
    
    @Default(value = "0")
    public Integer getFavCount();
    public void setFavCount(Integer favCount);
    
    @OneToMany
    public ZQLSharePermissions[] getZQLFilterSharePermissions();
    
    @OneToMany
    public ZQLFavoriteAsoc[] getZQLFilterFavoriteAsoc();    

}
