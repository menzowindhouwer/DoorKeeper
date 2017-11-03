/* 
 * Copyright (C) 2017 The Language Archive
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.mpi.tla.flat.deposit.action;

import java.net.URI;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmSequenceIterator;
import nl.mpi.tla.flat.deposit.Context;
import nl.mpi.tla.flat.deposit.DepositException;
import nl.mpi.tla.flat.deposit.sip.Collection;
import org.slf4j.LoggerFactory;

/**
 *
 * @author menzowi
 */
public class Locking extends AbstractAction {
    
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Locking.class.getName());
    
    private static Map<String,ReentrantLock> locks = new HashMap<>();
    
    private Set<ReentrantLock> locked = new HashSet<>();
    
    @Override
    public boolean perform(Context context) throws DepositException {
        String mode = this.getParameter("mode", "lock");
        if (mode.equals("lock")) {
            for (XdmSequenceIterator iter=(params.containsKey("what")?params.get("what"):new XdmAtomicValue("sip")).iterator();iter.hasNext();) {
                String what = iter.next().getStringValue();
                if (what.equals("sip")) {
                    String uri = context.getSIP().getFID(true).toString();
                    if (!locks.containsKey(uri))
                        locks.put(uri, new ReentrantLock());
                    ReentrantLock lock = locks.get(uri);
                    lock.lock();
                    locked.add(lock);
                } else if (what.contains("collections")) {
                    // loop over collections
                    for (Collection col:context.getSIP().getCollections(!mode.contains("parent"))) {
                        String uri = col.getFID(true).toString();
                        if (!locks.containsKey(uri))
                            locks.put(uri, new ReentrantLock());
                        ReentrantLock lock = locks.get(uri);
                        lock.lock();
                        locked.add(lock);
                    }
                } else {
                    if (!locks.containsKey(what))
                        locks.put(what, new ReentrantLock());
                    ReentrantLock lock = locks.get(what);
                    lock.lock();
                    locked.add(lock);
                }
            }
            return true;
        }
        if (mode.equals("unlock")) {
            for (ReentrantLock lock:locked) {
                lock.unlock();
                // TODO: remove unused locks?
            }
            return true;
        }
        logger.error("Unknown locking mode!");
        return false;
    }
        
}
