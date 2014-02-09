(require 'package)
(package-initialize)
(require 'org)
(setq org-publish-project-alist
      '(
	("docs" :components ("org-notes" "css" "images"))
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
	 :auto-preamble t
	 :html-postamble nil
	 )))
(org-publish "docs" t)
