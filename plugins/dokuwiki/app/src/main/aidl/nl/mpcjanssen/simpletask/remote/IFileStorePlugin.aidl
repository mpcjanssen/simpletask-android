// IFileStorePlugin.aidl
package nl.mpcjanssen.simpletask.remote;

interface IFileStorePlugin {
   boolean isAuthenticated();
   String loadTasksFromFile(String path, out List<String> lines) ;
   String saveTasksToFile(String path, in List<String> lines, String eol, boolean append);
   void login();
   void logout();
   String readFile(String path);
   void writeFile(String path, String contents);
   String getRemoteVersion(String path);
   String getDefaultPath();
   boolean loadFileList(String path, boolean txtOnly, out List<String> folders, out List<String> files );
}