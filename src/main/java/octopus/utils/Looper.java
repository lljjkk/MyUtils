package octopus.utils;

import groovy.lang.Binding;

public class Looper {
	public int index = -1;
	public SnippetRunner shell = null;
	public boolean doContinue = true;

	public void initSnippetContext(Binding context) {
	}

	public void before() {
	}

	public void after() {
	}
}
