package org.springframework.data.xap.repository.support;

import com.gigaspaces.query.IdQuery;
import com.gigaspaces.query.IdsQuery;
import com.gigaspaces.query.aggregators.AggregationSet;
import com.google.common.collect.Lists;
import com.j_spaces.core.client.SQLQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.xap.repository.XapRepository;
import org.springframework.data.xap.repository.query.QueryBuilder;
import org.springframework.data.xap.spaceclient.SpaceClient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Oleksiy_Dyagilev
 */
public class SimpleXapRepository<T, ID extends Serializable> implements XapRepository<T, ID> {

    private SpaceClient space;
    private EntityInformation<T, ID> entityInformation;

    public SimpleXapRepository(SpaceClient space, EntityInformation<T, ID> entityInformation) {
        this.space = space;
        this.entityInformation = entityInformation;
    }

    @Override
    public <S extends T> S save(S entity) {
        space.write(entity);
        // TODO: think about auto-generated id
        return entity;
    }

    @Override
    public <S extends T> Iterable<S> save(Iterable<S> entities) {
        space.writeMultiple(toArray(entities));
        // TODO: think about auto-generated id
        return entities;
    }

    @Override
    public T findOne(ID id) {
        Class<T> aClass = entityInformation.getJavaType();
        return space.readById(aClass, id);
    }

    @Override
    public boolean exists(ID id) {
        return findOne(id) != null;
    }

    @Override
    public Iterable<T> findAll() {
        Class<T> aClass = entityInformation.getJavaType();
        SQLQuery<T> query = new SQLQuery<>(aClass, "");
        T[] found = space.readMultiple(query);
        return new ArrayList<>(Arrays.asList(found));
    }

    @Override
    public Iterable<T> findAll(Iterable<ID> ids) {
        Class<T> aClass = entityInformation.getJavaType();
        return space.readByIds(aClass, toArray(ids));
    }

    @Override
    public long count() {
        Class<T> aClass = entityInformation.getJavaType();
        SQLQuery<T> query = new SQLQuery<>(aClass, "");
        // Changed from QueryExtension.count(space, query, "");
        return space.aggregate(query, new AggregationSet().count("")).getLong(0);
    }

    @Override
    public void delete(ID id) {
        Class<T> aClass = entityInformation.getJavaType();
        IdQuery<T> idQuery = new IdQuery<T>(aClass, id).setProjections("");
        space.takeById(idQuery);
    }

    @Override
    public void delete(T entity) {
        delete(entityInformation.getId(entity));
    }

    @Override
    public void delete(Iterable<? extends T> entities) {
        // TODO: consider replacing with distributed task to not return deleted entities back on client
        List<ID> idList = new ArrayList<>();
        for (T entity : entities) {
            ID id = entityInformation.getId(entity);
            idList.add(id);
        }
        Object[] idArray = idList.toArray();
        Class<T> aClass = entityInformation.getJavaType();
        IdsQuery<T> idsQuery = new IdsQuery<T>(aClass, idArray).setProjections("");
        space.takeByIds(idsQuery);
    }

    @Override
    public void deleteAll() {
        // TODO: consider replacing with distributed task to not return deleted entities back on client
        Class<T> aClass = entityInformation.getJavaType();
        SQLQuery<T> query = new SQLQuery<>(aClass, "").setProjections("");
        space.takeMultiple(query);
    }

    @SuppressWarnings("unchecked")
    private <E> E[] toArray(Iterable<E> elems) {
        ArrayList<E> arrayList = new ArrayList<E>();
        for (E elem : elems) {
            arrayList.add(elem);
        }
        return (E[]) arrayList.toArray();
    }

    @Override
    public Iterable<T> findAll(Sort sort) {
        return findAllSortedInternal(sort, 0);
    }

    @Override
    public Page<T> findAll(Pageable pageable) {
        int pageSize = pageable.getPageSize();
        int offset = pageable.getOffset();
        List<T> allSortedInternal = findAllSortedInternal(pageable.getSort(), offset + pageSize);
        List<T> result = (offset < allSortedInternal.size()) ? allSortedInternal.subList(offset, allSortedInternal.size()) : Collections.<T>emptyList() ;
        return new PageImpl<T>(result);
    }

    private List<T> findAllSortedInternal(Sort sort, int count){
        //TODO: null handling, ignore case
        Class<T> aClass = entityInformation.getJavaType();
        StringBuilder stringBuilder = new StringBuilder("");
        if (count > 0 ){
            stringBuilder.append(" rownum <=").append(count);
        }
        stringBuilder.append(new QueryBuilder(sort).buildQuery());
        SQLQuery<T> query = new SQLQuery<>(aClass, stringBuilder.toString());
        T[] entities = space.readMultiple(query);
        return Lists.newArrayList(entities);
    }
}
