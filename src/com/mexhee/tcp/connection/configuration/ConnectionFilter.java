package com.mexhee.tcp.connection.configuration;

import java.util.HashSet;
import java.util.Set;

//http://www.winpcap.org/docs/docs_40_2/html/group__language.html
public class ConnectionFilter {

	private Set<Criteria> criterias = new HashSet<ConnectionFilter.Criteria>();

	public void addDestintationHost(String destinationHost) {
		criterias.add(new Criteria("dst host", destinationHost));
	}

	public void addSourceHost(String sourceHost) {
		criterias.add(new Criteria("src host", sourceHost));
	}

	public void addDestintationPort(int destinationPort) {
		criterias.add(new Criteria("dst port", destinationPort + ""));
	}

	public void addSourcePort(int sourcePort) {
		criterias.add(new Criteria("dst port", sourcePort + ""));
	}

	public void addCustomizedCriteria(String name, String value) {
		criterias.add(new Criteria(name, value));
	}
	//one of the host
	public void addHost(String host){
		criterias.add(new Criteria("host", host));
	}

	static class Criteria {
		String name;
		String value;

		public Criteria(String name, String value) {
			this.name = name;
			this.value = value;
		}

		@Override
		public String toString() {
			return name + " " + value;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Criteria other = (Criteria) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer(3 + criterias.size() * 20);
		sb.append("tcp");
		for (Criteria criteria : criterias) {
			sb.append(" and " + criteria.toString());
		}
		return sb.toString();
	}
}
