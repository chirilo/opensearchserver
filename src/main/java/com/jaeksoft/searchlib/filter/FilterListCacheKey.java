/**   
 * License Agreement for OpenSearchServer
 *
 * Copyright (C) 2008-2013 Emmanuel Keller / Jaeksoft
 * 
 * http://www.open-search-server.com
 * 
 * This file is part of OpenSearchServer.
 *
 * OpenSearchServer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * OpenSearchServer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with OpenSearchServer. 
 *  If not, see <http://www.gnu.org/licenses/>.
 **/

package com.jaeksoft.searchlib.filter;

import java.io.IOException;
import java.util.Iterator;
import java.util.TreeSet;

import com.jaeksoft.searchlib.SearchLibException;
import com.jaeksoft.searchlib.analysis.PerFieldAnalyzer;
import com.jaeksoft.searchlib.function.expression.SyntaxError;
import com.jaeksoft.searchlib.query.ParseException;
import com.jaeksoft.searchlib.request.AbstractSearchRequest;
import com.jaeksoft.searchlib.schema.SchemaField;
import com.jaeksoft.searchlib.webservice.query.search.SearchQueryAbstract.OperatorEnum;

public class FilterListCacheKey implements Comparable<FilterListCacheKey> {

	private final TreeSet<FilterCacheKey> filterCacheKeySet;
	private final OperatorEnum defaultOperator;

	public FilterListCacheKey(FilterList filterList, SchemaField defaultField,
			PerFieldAnalyzer analyzer, AbstractSearchRequest request)
			throws ParseException, SyntaxError, SearchLibException, IOException {
		filterCacheKeySet = new TreeSet<FilterCacheKey>();
		if (filterList != null) {
			for (FilterAbstract<?> filter : filterList)
				filterCacheKeySet.add(new FilterCacheKey(filter, defaultField,
						analyzer, request));
			defaultOperator = filterList.getDefaultOperator();
		} else
			defaultOperator = OperatorEnum.AND;
	}

	@Override
	public int compareTo(FilterListCacheKey o) {
		int i1 = filterCacheKeySet.size();
		int i2 = o.filterCacheKeySet.size();
		if (i1 < i2)
			return -1;
		else if (i1 > i2)
			return 1;
		if (defaultOperator != o.defaultOperator)
			return defaultOperator.ordinal() - o.defaultOperator.ordinal();
		Iterator<FilterCacheKey> it = o.filterCacheKeySet.iterator();
		for (FilterCacheKey filterCacheKey : filterCacheKeySet) {
			int c = filterCacheKey.compareTo(it.next());
			if (c != 0)
				return c;
		}
		return 0;
	}

}
