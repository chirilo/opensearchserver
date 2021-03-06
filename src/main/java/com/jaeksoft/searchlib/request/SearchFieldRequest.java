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

package com.jaeksoft.searchlib.request;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.jaeksoft.searchlib.SearchLibException;
import com.jaeksoft.searchlib.config.Config;
import com.jaeksoft.searchlib.facet.FacetField;
import com.jaeksoft.searchlib.facet.Range;
import com.jaeksoft.searchlib.function.expression.SyntaxError;
import com.jaeksoft.searchlib.query.ParseException;
import com.jaeksoft.searchlib.snippet.SnippetField;
import com.jaeksoft.searchlib.snippet.SnippetFieldList;
import com.jaeksoft.searchlib.util.DomUtils;
import com.jaeksoft.searchlib.util.XPathParser;
import com.jaeksoft.searchlib.util.XmlWriter;
import com.jaeksoft.searchlib.webservice.query.search.SearchFieldQuery.SearchField.Mode;
import com.jaeksoft.searchlib.webservice.query.search.SearchQueryAbstract.FragmenterEnum;
import com.jaeksoft.searchlib.webservice.query.search.SearchQueryAbstract.OperatorEnum;

public class SearchFieldRequest extends AbstractSearchRequest implements
		RequestInterfaces.ReturnedFieldInterface,
		RequestInterfaces.FilterListInterface {

	public final static String SEARCHFIELD_QUERY_NODE_NAME = "query";

	private List<SearchField> searchFields;

	private Map<String, String> queryStringMap;

	public SearchFieldRequest() {
		super(null, RequestTypeEnum.SearchFieldRequest);
	}

	public SearchFieldRequest(Config config) {
		super(config, RequestTypeEnum.SearchFieldRequest);
	}

	@Override
	protected void setDefaultValues() {
		super.setDefaultValues();
		searchFields = new ArrayList<SearchField>(0);
		queryStringMap = null;
	}

	public void setQueryString(String field, String queryString) {
		if (queryStringMap == null)
			queryStringMap = new TreeMap<String, String>();
		if (StringUtils.isEmpty(queryString))
			queryStringMap.remove(field);
		else
			queryStringMap.put(field, queryString);
	}

	public void setQueryString(Map<String, String> queryStringMap) {
		if (this.queryStringMap == null)
			this.queryStringMap = new TreeMap<String, String>();
		else
			this.queryStringMap.clear();
		this.queryStringMap.putAll(queryStringMap);
	}

	final protected String getQueryString(String field) {
		if (queryStringMap == null)
			return null;
		return queryStringMap.get(field);
	}

	@Override
	public void copyFrom(AbstractRequest request) {
		super.copyFrom(request);
		SearchFieldRequest searchFieldRequest = (SearchFieldRequest) request;
		this.searchFields = new ArrayList<SearchField>(0);
		if (searchFieldRequest.searchFields != null)
			for (SearchField searchField : searchFieldRequest.searchFields)
				this.searchFields.add(searchField.clone());
		if (searchFieldRequest.queryStringMap == null)
			this.queryStringMap = null;
		else
			this.queryStringMap = new TreeMap<String, String>(
					searchFieldRequest.queryStringMap);
	}

	final private Query getComplexQuery(final Collection<Query> queries) {
		BooleanQuery complexQuery = new BooleanQuery();
		for (Query query : queries)
			complexQuery.add(query, Occur.SHOULD);
		return complexQuery;
	}

	@Override
	protected Query newSnippetQuery(String queryString) throws IOException,
			ParseException, SyntaxError, SearchLibException {
		SnippetFieldList snippetFieldList = getSnippetFieldList();
		List<Query> queries = new ArrayList<Query>(searchFields.size());
		for (SearchField searchField : searchFields) {
			String field = searchField.getField();
			String query = getQueryString(field);
			if (query == null)
				query = queryString;
			if (snippetFieldList.get(field) != null) {
				searchField.addQuery(analyzer, query, queries, phraseSlop,
						Occur.SHOULD);
			}
		}
		return getComplexQuery(queries);
	}

	private Query newComplexQuery(String queryString, Occur occur)
			throws ParseException, SyntaxError, SearchLibException, IOException {
		if (emptyReturnsAll && StringUtils.isEmpty(queryString))
			return new MatchAllDocsQuery();
		List<Query> queries = new ArrayList<Query>(searchFields.size());
		for (SearchField searchField : searchFields) {
			String query = getQueryString(searchField.getField());
			if (query == null)
				query = queryString;
			searchField.addQuery(analyzer, query, queries, phraseSlop, occur);
		}
		return getComplexQuery(queries);
	}

	@Override
	protected Query newComplexQuery(String queryString) throws ParseException,
			SyntaxError, SearchLibException, IOException {
		Occur occur = defaultOperator == OperatorEnum.AND ? Occur.MUST
				: Occur.SHOULD;
		return newComplexQuery(queryString, occur);
	}

	@Override
	public void fromXmlConfigNoLock(Config config, XPathParser xpp,
			Node requestNode) throws XPathExpressionException, DOMException,
			ParseException, InstantiationException, IllegalAccessException,
			ClassNotFoundException {
		super.fromXmlConfigNoLock(config, xpp, requestNode);
		Node fieldQueryNode = DomUtils.getFirstNode(requestNode,
				SEARCHFIELD_QUERY_NODE_NAME);
		if (fieldQueryNode != null) {
			List<Node> fieldNodeList = DomUtils.getNodes(fieldQueryNode,
					SearchField.SEARCHFIELD_NODE_NAME);
			if (fieldNodeList != null)
				for (Node fieldNode : fieldNodeList)
					searchFields.add(new SearchField(fieldNode));
		}
	}

	@Override
	public void writeSubXmlConfig(XmlWriter xmlWriter) throws SAXException {
		xmlWriter.startElement(SEARCHFIELD_QUERY_NODE_NAME);
		for (SearchField searchField : searchFields)
			searchField.writeXmlConfig(xmlWriter);
		xmlWriter.endElement();
	}

	@Override
	public String getInfo() {
		rwl.r.lock();
		try {
			return searchFields.toString();
		} finally {
			rwl.r.unlock();
		}
	}

	public Collection<SearchField> getSearchFields() {
		rwl.r.lock();
		try {
			return searchFields;
		} finally {
			rwl.r.unlock();
		}
	}

	public void add(SearchField searchField) {
		rwl.w.lock();
		try {
			searchFields.add(searchField);
			resetNoLock();
		} finally {
			rwl.w.unlock();
		}
	}

	/**
	 * Add a new search field to the request
	 * 
	 * @param fieldName
	 *            The name of the field
	 * @param phrase
	 *            Activate the phrase search
	 * @param boost
	 *            Set the boost for the term search
	 * @param phraseBoost
	 *            Set the boost for the phrase search
	 */
	public void addSearchField(String fieldName, Mode mode, double termBoost,
			double phraseBoost, Integer phraseSlop) {
		add(new SearchField(fieldName, mode, termBoost, phraseBoost, phraseSlop));
	}

	public void addSearchField(String fieldName, double termBoost) {
		add(new SearchField(fieldName, Mode.PATTERN, termBoost, 1.0, null));
	}

	public void remove(SearchField searchField) {
		rwl.w.lock();
		try {
			searchFields.remove(searchField);
			resetNoLock();
		} finally {
			rwl.w.unlock();
		}
	}

	/**
	 * Add a snippet field
	 * 
	 * @param fieldName
	 *            The name of the field
	 * @param fragmenter
	 *            The fragmentation method
	 * @param maxSize
	 *            The maximum size of the snippet in character
	 * @param separator
	 *            The string sequence used to highlight keywords
	 * @param maxNumber
	 *            The maximum number of snippet
	 * @throws SearchLibException
	 */
	public void addSnippetField(String fieldName, FragmenterEnum fragmenter,
			int maxSize, String separator, int maxNumber)
			throws SearchLibException {
		SnippetField field = new SnippetField(fieldName);
		field.setFragmenter(fragmenter.className);
		field.setMaxSnippetSize(maxSize);
		field.setSeparator(separator);
		field.setMaxSnippetNumber(maxNumber);
		this.getSnippetFieldList().put(field);
	}

	/**
	 * Add facet
	 * 
	 * @param fieldName
	 *            The name of the field
	 * @param minCount
	 *            The minimum number of document
	 * @param multivalued
	 *            The field can contains several values
	 * @param postCollapsing
	 *            The number is calculated after collapsing
	 * @param ranges
	 *            An optional list of ranges
	 */
	public void addFacet(String fieldName, int minCount, boolean multivalued,
			boolean postCollapsing, List<Range> ranges) {
		FacetField facetField = new FacetField(fieldName, minCount,
				multivalued, postCollapsing, ranges);
		getFacetFieldList().put(facetField);
	}

}
