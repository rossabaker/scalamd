/*
 * Copyright (C) 2009-2010 Boris Okunskiy (http://incarnate.ru)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL AUTHOR OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

package ru.circumflex.orm

import collection.mutable.ListBuffer
import Query._

/**
 * Criteria is high-level abstraction of <code>Select</code>. It is used to operate
 * specifically with records and simplifies record querying.
 * Note that if you want to use projections, you should still use lower-level <code>Select</code>
 */
class Criteria[R](val relation: Relation[R]) {

  protected val rootNode = relation.as("root")

  protected val rootTree: RelationNode[R] = rootNode
  protected val projections: Seq[Projection[_]] = rootNode.projections

  protected val restrictions = new ListBuffer[Predicate]()
  protected var _orders: Seq[Order] = Nil
  protected var _limit = -1;
  protected var _offset = 0;

  protected def prepareQuery = Query.select(projections: _*)
          .from(rootTree)
          .limit(_limit)
          .offset(_offset)
          .where(preparePredicate)
          .addOrder(_orders: _*)

  protected def preparePredicate: Predicate =
    if (restrictions.size > 0)
      and(restrictions: _*)
    else EmptyPredicate

  /**
   * Sets the maximum results for this query.
   */
  def limit(value: Int): this.type = {
    _limit = value
    return this
  }

  /**
   * Sets the result offset for this query.
   */
  def offset(value: Int): this.type = {
    _offset = value
    return this
  }

  /**
   * Adds arbitrary predicate to this query's restrictions list.
   * Restrictions are assembled into conjunction prior to query execution.
   */
  def add(predicate: Predicate): this.type = {
    restrictions += predicate
    return this
  }

  /**
   * Adds a predicate related to criteria root node to this query's restrictions list.
   * Restrictions are assembled into conjunction prior to query execution.
   */
  def add(nodeToPredicate: RelationNode[R] => Predicate): this.type = {
    restrictions += nodeToPredicate(rootNode)
    return this
  }

  /**
   * Adds an order to ORDER BY clause.
   */
  def addOrder(orders: Order*): this.type = {
    _orders ++= orders.toList
    return this
  }

  /**
   * Executes the query and retrieves records list.
   */
  def list: Seq[R] = prepareQuery.list.map(_.apply(0).asInstanceOf[R])

  /**
   * Executes the query and retrieves unqiue record.
   * An exception is thrown if result set yields more than one row.
   */
  def unique: Option[R] = prepareQuery.unique.map(_.apply(0).asInstanceOf[R])

  /**
   * Executes a query and retrieves the first record.
   */
  def first: Option[R] = prepareQuery.first.map(_.apply(0).asInstanceOf[R])

  /**
   * Executes the DELETE statement for this relation using specified criteria.
   */
  def delete: Int = new Delete(relation).where(preparePredicate).executeUpdate

}