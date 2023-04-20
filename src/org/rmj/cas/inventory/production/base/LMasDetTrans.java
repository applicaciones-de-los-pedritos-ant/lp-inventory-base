/**
 * @author Michael Cuison 2021.06.22
 */

package org.rmj.cas.inventory.production.base;

public interface LMasDetTrans {
    void MasterRetreive(int fnIndex, Object foValue);
    void DetailRetreive(int fnRow, int fnIndex, Object foValue);
}
