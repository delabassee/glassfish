
package Data;

import javax.ejb.*;

/**
 * Created Dec 16, 2002 1:22:07 PM
 * Code generated by the Forte For Java EJB Builder
 * @author mvatkina
 */

public interface LocalSuppliersHome extends javax.ejb.EJBLocalHome {
    
    public Data.LocalSuppliers findByPrimaryKey(Data.SuppliersKey aKey)
    throws javax.ejb.FinderException;
    
    public LocalSuppliers create(java.lang.Integer partid, java.lang.Integer supplierid, java.lang.String name, int status, java.lang.String city) throws javax.ejb.CreateException;
    
    public java.util.Collection findAll() throws javax.ejb.FinderException;
    
}
