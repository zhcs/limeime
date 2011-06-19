package net.toload.main.hd;

interface ISearchService
{
		void initial();
		String getTablename();
		void setTablename(String tablename, boolean numberMapping, boolean symbolMapping);
		List query(String code, boolean softkeyboard);
		void rQuery(String word);
		List queryUserDic(String word);
		void updateMapping(String id, String code, String word, String pword, int score, boolean isDictionary);
		void addUserDict(String id, String code, String word, String pword, int score, boolean isDictionary);
		void updateUserDict();
		String hanConvert(String input);
		String keyToKeyname(String code);
		List getKeyboardList();
		List getImList();
		void clear();
		List queryDictionary(String word);
		void setSelectedText(String text);
		String getSelectedText();
		void close();
		boolean isImKeys(char c);
		String getSelkey();
		int isSelkey(char c);

}