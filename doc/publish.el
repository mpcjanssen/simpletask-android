(require 'package)
(package-initialize)
(require 'org)
(require 'ox-md)

(defun org-md-publish-to-md (plist filename pub-dir)
  "Publish an org file to Markdown.

FILENAME is the filename of the Org file to be published.  PLIST
is the property list for the given project.  PUB-DIR is the
publishing directory.

Return output file name."
  (org-publish-org-to 'md filename ".md"
		      plist pub-dir))

(setq org-html-head-include-default-style nil
      org-html-head-include-scripts nil)



(setq org-html-head
        "<link rel=\"stylesheet\" type=\"text/css\" href=\"css/style.css\" />")

(setq org-publish-project-alist
      '(
	("docs" :components ("org-notes" "css" "images" "markdown"))
	("css"
               :base-directory "./css/"
               :base-extension "css"
               :publishing-directory "../src/main/assets/css/"
               :publishing-function org-publish-attachment)
	("images"
               :base-directory "./images/"
               :base-extension "png"
               :publishing-directory "../src/main/assets/images/"
               :publishing-function org-publish-attachment)
	("org-notes"
	 :base-directory "."
	 :base-extension "org"
	 :publishing-directory "../src/main/assets/"
	 :publishing-function org-html-publish-to-html
	 :recursive t
	 :headline-levels 4             ; Just the default for this project.
	 :auto-preamble f
	 :html-postamble nil
	 :html-head-include-default-style nil
	 )
	("markdown"
	 :base-directory "."
	 :base-extension "org"
	 :publishing-directory "../markdown"
	 :publishing-function org-md-publish-to-md
	 :recursive t
	 :headline-levels 4             ; Just the default for this project.
	 :auto-preamble f
	 :html-postamble nil
	 )))
(org-publish "docs" t)
