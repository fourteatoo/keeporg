[![CircleCI](https://dl.circleci.com/status-badge/img/gh/fourteatoo/keeporg/tree/main.svg?style=svg)](https://dl.circleci.com/status-badge/redirect/gh/fourteatoo/keeporg/tree/main)
[![Coverage Status](https://coveralls.io/repos/github/fourteatoo/keeporg/badge.svg)](https://coveralls.io/github/fourteatoo/keeporg)


# keeporg

Convert a Google Keep takeout to Emacs Org (or Markdown) notes.

## Installation

To compile

    $ lein uberjar



## Usage

	$ java -jar target/keeporg<version_number>-standalone.jar takeout_file ...

to convert your takeout files.

## Options

	$ java -jar target/keeporg<version_number>-standalone.jar -h
	
will get you a short usage print

## Examples

	$ java -jar target/keeporg<version_number>-standalone.jar takeout-20251020T091417Z-1-001.tgz
	
will create a `takeout-20251020T091417Z-1-001.org` file with all your
notes inside.

On the other hand

	$ java -jar target/keeporg<version_number>-standalone.jar -s takeout-20251020T091417Z-1-001.tgz
	
will create an org file for each file inside the takeout archive.
Each file will have the same name as the file inside the archive; just
with the extention changed to ".org".


### Bugs

The Markdown output is less complete than the Emacs Org one.  Ideas
are welcome.

## License

Copyright © 2025 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
