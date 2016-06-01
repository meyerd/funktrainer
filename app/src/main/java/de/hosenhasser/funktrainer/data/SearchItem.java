/*  vim: set sw=4 tabstop=4 fileencoding=UTF-8:
 *
 *  Copyright 2014 Matthias Wimmer
 *            2015 Dominik Meyer
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package de.hosenhasser.funktrainer.data;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class SearchItem implements Comparable<SearchItem> {
	public int id;
	public String label;
	public String reference;
	public String categoryname;
    public String topicname;

	public SearchItem(int id, String label, String reference,
                      String categoryname, String topicname) {
		this.id = id;
		this.label = label;
		this.reference = reference;
		this.categoryname = categoryname;
        this.topicname = topicname;
	}

	@Override
	public int hashCode() {
		return this.label.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SearchItem other = (SearchItem)obj;
		if (other.label == this.label)
			return true;
		return false;
	}

    public int compareTo(SearchItem o) {
        return this.label.compareTo(o.label);
    }
}
